/*
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.resolve.android

import java.util.ArrayList
import javax.xml.parsers.SAXParserFactory
import java.io.File
import java.io.FileInputStream
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.JetFile
import javax.xml.parsers.SAXParser
import java.util.HashMap
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileAdapter
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentLinkedQueue
import com.intellij.openapi.util.Key
import com.intellij.testFramework.LightVirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.psi.PsiManager
import java.io.FileInputStream
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.Attributes
import org.jetbrains.kotlin.resolve.android.AndroidConst.*
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.impl.PsiModificationTrackerImpl
import java.util.Queue
import com.intellij.psi.PsiFile
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import com.intellij.openapi.diagnostic.Log

abstract class AndroidUIXmlParser {

    inner class NoUIXMLsFound: Exception("No android UI xmls found in $searchPath")
    class NoAndroidManifestFound: Exception("No android manifest file found in project root")
    class ManifestParsingFailed

    enum class CacheAction { HIT; MISS }

    val androidImports = arrayListOf("android.app.Activity",
                                     "android.view.View",
                                     "android.widget.*")

    protected abstract val searchPath: String?
    protected abstract val androidAppPackage: String

    private val saxParser = initSAX()

    private val fileCache = HashMap<PsiFile, String>()
    private var lastCachedPsi: JetFile? = null
    private val fileModificationTime = HashMap<PsiFile, Long>()

    protected val filesToProcess: Queue<PsiFile> = ConcurrentLinkedQueue()
    protected var listenerSetUp: Boolean = false
    protected volatile var invalidateCaches: Boolean = false

    public fun parseToString(): String? {
        val cacheState = doParse()
        if (cacheState == null) return null
        return renderString()
    }

    public fun parseToPsi(project: Project): JetFile? {
        populateQueue(project)
        val cacheState = doParse()
        if (cacheState == null) return null
        return if (cacheState == CacheAction.MISS || lastCachedPsi == null) {
            try {
                val vf = LightVirtualFile(SYNTHETIC_FILENAME, renderString())
                val psiFile = PsiManager.getInstance(project).findFile(vf) as JetFile
                psiFile.putUserData(ANDROID_SYNTHETIC, "OK")
                psiFile.putUserData(ANDROID_USER_PACKAGE, androidAppPackage)
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
        kw.writePackage(androidAppPackage)
        for (elem in androidImports)
            kw.writeImport(elem)
        kw.writeEmptyLine()
        return kw
    }

    private fun parseSingleFileWithCache(file: PsiFile): Pair<String, CacheAction> {
        val lastRecorded = fileModificationTime[file] ?: -1
        if (file.getModificationStamp() > lastRecorded)
            return Pair(parseSingleFile(file), CacheAction.MISS)
        else
            return Pair(fileCache[file]!!, CacheAction.HIT)
    }

    private fun parseSingleFile(file: PsiFile): String {
        val ids: MutableCollection<AndroidWidget> = ArrayList()
        val handler = AndroidXmlHandler({ id, wClass -> ids.add(AndroidWidget(id, wClass)) })
        fileModificationTime[file] = file.getModificationStamp()
        try {
            val source = InputSource(ByteArrayInputStream(file.getText()!!.getBytes("utf-8")))
            saxParser.parse(source, handler)
            val res = produceKotlinProperties(KotlinStringWriter(), ids).toString()
            fileCache[file] = res
            return res
        } catch (e: Exception) {
            Log.print(e.getMessage())
            invalidateCaches()
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

    protected fun populateQueue(project: Project) {
        val fileManager = VirtualFileManager.getInstance()
        val watchDir = fileManager.findFileByUrl("file://" + searchPath)
        val psiManager = PsiManager.getInstance(project)
        filesToProcess.addAll(watchDir?.getChildren()?.toArrayList()?.map { psiManager.findFile(it) } ?.mapNotNull { it })
    }

    protected abstract fun lazySetup()

    protected fun readManifest(): AndroidManifest {
        try {
            val manifestXml = File(searchPath!!).getParentFile()!!.getParentFile()!!.listFiles { it.name == "AndroidManifest.xml" }!!.first()
            var _package: String = ""
            try {
                saxParser.parse(FileInputStream(manifestXml), object : DefaultHandler() {
                    override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
                        if (localName == "manifest")
                            _package = attributes.toMap()["package"] ?: ""
                    }
                })
            } catch (e: Exception) {
                throw e
            }
            return AndroidManifest(_package)
        } catch (e: Exception) {
            throw NoAndroidManifestFound()
        }
    }

    private fun produceKotlinProperties(kw: KotlinStringWriter, ids: Collection<AndroidWidget>): StringBuffer {
        for (id in ids) {
            val body = arrayListOf("return findViewById(0) as ${id.className}")
            kw.writeImmutableExtensionProperty(receiver = "Activity",
                                      name = id.id,
                                      retType = id.className,
                                      getterBody = body )
        }
        return kw.output()
    }

}
