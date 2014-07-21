package org.jetbrains.jet.lang.resolve.android

import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.psi.JetPsiFactory
import java.util.ArrayList
import java.util.HashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.io.File
import javax.xml.parsers.SAXParserFactory
import javax.xml.parsers.SAXParser
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileAdapter
import com.intellij.openapi.vfs.VirtualFileEvent

abstract class AndroidUIXmlParser {

    inner class NoUIXMLsFound: Exception("No android UI xmls found in $searchPath")

    enum class CacheAction { HIT; MISS }

    val androidImports = arrayListOf("android.app.Activity",
                                     "android.view.View",
                                     "android.widget.*")
    abstract val searchPath: String?

    val saxParser = initSAX()

    val fileCache = HashMap<VirtualFile, String>()
    var lastCachedPsi: JetFile? = null
    val fileModificationTime = HashMap<VirtualFile, Long>()

    val filesToProcess = ConcurrentLinkedQueue<VirtualFile>()
    var listenerSetUp = false
    volatile var invalidateCaches = false

    public fun parseToString(): String? {
        val cacheState = doParse()
        if (cacheState == null) return null
        return renderString()
    }

    public fun parseToPsi(project: Project): JetFile? {
        val cacheState = doParse()
        if (cacheState == null) return null
        return if (cacheState == CacheAction.MISS || lastCachedPsi == null) {
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

    private fun parseSingleFileWithCache(file: VirtualFile): Pair<String, CacheAction> {
        val lastRecorded = fileModificationTime[file] ?: -1
        if (file.getModificationStamp() > lastRecorded)
            return Pair(parseSingleFile(file), CacheAction.MISS)
        else
            return Pair(fileCache[file]!!, CacheAction.HIT)
    }

    private fun parseSingleFile(file: VirtualFile): String {
        val ids: MutableCollection<AndroidWidget> = ArrayList()
        val handler = AndroidXmlHandler({ id, wClass -> ids.add(AndroidWidget(id, wClass)) })
        fileModificationTime[file] = file.getModificationStamp()
        try {
            saxParser.parse(file.getInputStream()!!, handler)
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
        lazySetup()
        if (invalidateCaches) invalidateCaches()
        var overallCacheMiss = false
        var file = filesToProcess.poll()
        while (file != null) {
            val res = parseSingleFileWithCache(file!!)
            overallCacheMiss = overallCacheMiss or (res.second == CacheAction.MISS)
            file = filesToProcess.poll()
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
        invalidateCaches = false
    }

    protected abstract fun lazySetup()

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
