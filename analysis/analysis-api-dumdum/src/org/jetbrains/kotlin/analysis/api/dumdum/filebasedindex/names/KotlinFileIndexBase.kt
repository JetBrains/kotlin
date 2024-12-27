package org.jetbrains.kotlin.analysis.api.dumdum.filebasedindex.names

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileContent
import com.intellij.util.io.KeyDescriptor
import org.jetbrains.kotlin.name.FqName
import java.util.*

abstract class KotlinFileIndexBase : ScalarIndexExtension<FqName>() {
    protected val LOG: Logger = Logger.getInstance(javaClass)

    override val keyDescriptor: KeyDescriptor<FqName>
        get() = FqNameKeyDescriptor

    protected fun indexer(f: (FileContent) -> FqName?): DataIndexer<FqName, Unit, FileContent> {
        return DataIndexer {
            try {
                val fqName = f(it)
                if (fqName != null) {
                    Collections.singletonMap<FqName, Unit>(fqName, null)
                } else {
                    emptyMap()
                }
            } catch (e: ProcessCanceledException) {
                throw e
            } catch (e: Throwable) {
                LOG.warn("Error while indexing file ${it.fileName}: ${e.message}")
                emptyMap()
            }
        }
    }
}