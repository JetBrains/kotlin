package org.jetbrains.jet.lang.resolve.android

import org.jetbrains.jet.lang.psi.JetFile
import java.util.ArrayList
import javax.xml.parsers.SAXParserFactory
import java.io.File
import java.io.FileInputStream
import com.intellij.openapi.project.Project
import org.jetbrains.jet.lang.psi.JetPsiFactory
import javax.xml.parsers.SAXParser
import java.util.HashMap
import com.intellij.openapi.vfs.VirtualFile

abstract class AndroidUIXmlParser {

    inner class NoUIXMLsFound: Exception("No android UI xmls found in $searchPath")

    enum class CacheAction { HIT; MISS }

    val androidImports = arrayListOf("android.app.Activity",
                                     "android.view.View",
                                     "android.widget.*")
    abstract val searchPath: String?

    val saxParser = initSAX()

    val fileCache = HashMap<File, String>()
    var lastCachedPsi: JetFile? = null
    val fileModificationTime = HashMap<File, Long>()

    public fun parseToString(): String? {
        val cacheState = doParse()
        if (cacheState == null) return null
        return renderString()
    }

    public fun parseToPsi(project: Project): JetFile? {
        val cacheState = doParse()
        if (cacheState == null) return null
        return if (cacheState === CacheAction.MISS) {
            try {
                val psiFile = JetPsiFactory(project).createFile(renderString())
                lastCachedPsi = psiFile
                psiFile
            } catch (e: Exception) {
                invalidateCaches()
                null
            }
        } else lastCachedPsi
    }

    private fun isAndroidUIXml(file: File): Boolean {
        return file.extension == "xml"
    }

    private fun initSAX(): SAXParser {
        val saxFactory = SAXParserFactory.newInstance()
        saxFactory?.setNamespaceAware(true)
        return saxFactory!!.newSAXParser()
    }

    private fun searchForUIXml(path: String): Collection<File> {
        return searchForUIXml(arrayListOf(File(path)))
    }

    private fun searchForUIXml(paths: Collection<File>?): Collection<File> {
        if (paths == null) return ArrayList(0)
        val res = ArrayList<File>()
        for (path in paths) {
            if (!path.exists()) continue;
            if (path.isFile() && isAndroidUIXml(path)) {
                res.add(path)
            } else if (path.isDirectory()) {
                res.addAll(searchForUIXml(path.listFiles()?.toArrayList()))
            }
        }
        return res
    }

    private fun writeImports(kw: KotlinStringWriter): KotlinWriter {
        for (elem in androidImports)
            kw.writeImport(elem)
        kw.writeEmptyLine()
        return kw
    }

    private fun parseSingleFileWithCache(file: File): Pair<String, CacheAction> {
        val lastRecorded = fileModificationTime[file] ?: -1
        if (file.lastModified() > lastRecorded)
            return Pair(parseSingleFile(file), CacheAction.MISS)
        else
            return Pair(fileCache[file]!!, CacheAction.HIT)
    }

    private fun parseSingleFile(file: File): String {
        val ids: MutableCollection<AndroidWidget> = ArrayList()
        val handler = AndroidXmlHandler({ id, wClass -> ids.add(AndroidWidget(id, wClass)) })
        fileModificationTime[file] = file.lastModified()
        try {
            saxParser.parse(FileInputStream(file), handler)
            val res = produceKotlinProperties(KotlinStringWriter(), ids).toString()
            fileCache[file] = res
            return res
        } catch (e: Exception) {
            fileCache[file] = ""
            return ""
        }
    }

    private fun doParse(): CacheAction? {
        if (searchPath == null || searchPath == "") return null
        val files = searchForUIXml(searchPath!!)
        var overallCacheMiss = false
        for (file in files) {
            val res = parseSingleFileWithCache(file)
            overallCacheMiss = overallCacheMiss or (res.second == CacheAction.MISS)
        }
        return if (overallCacheMiss) CacheAction.MISS else CacheAction.HIT
    }

    private fun renderString(): String {
        val buffer = writeImports(KotlinStringWriter()).output()
        for (buf in fileCache.values())
            buffer.append(buf)
        return buffer.toString()
    }

    private fun invalidateCaches() {
        fileCache.clear()
        fileModificationTime.clear()
        lastCachedPsi = null
    }

    private fun produceKotlinProperties(kw: KotlinStringWriter, ids: Collection<AndroidWidget>): StringBuffer {
        for (id in ids) {
            val body = arrayListOf("return findViewById(R.id.${id.id}) as ${id.className}")
            kw.writeImmutableExtensionProperty(receiver = "Activity",
                                      name = id.id,
                                      retType = id.className,
                                      getterBody = body )
        }
        return kw.output()
    }

}
