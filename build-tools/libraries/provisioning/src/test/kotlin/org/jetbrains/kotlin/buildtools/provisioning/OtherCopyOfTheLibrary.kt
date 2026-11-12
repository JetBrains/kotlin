/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.provisioning

import java.io.Closeable
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path

/**
 * A second copy of this library in its own class loader, as when a Gradle daemon has loaded KGP twice. Its classes have the
 * same names as the ones the tests use, but they are other classes, with their own static state; the Kotlin stdlib is shared.
 * The copy is driven through reflection, since its types are not the test's types.
 */
internal class OtherCopyOfTheLibrary : Closeable {
    private val loader = LibraryFirstClassLoader(InstallationStore::class.java.protectionDomain.codeSource.location)

    val storeClass: Class<*> = loader.loadClass(InstallationStore::class.java.name)
    private val leftoverPolicyClass = loader.loadClass(LeftoverPolicy::class.java.name)
    private val listenerClass = loader.loadClass(ProvisioningListener::class.java.name)
    private val stagingAreaClass = loader.loadClass(StagingArea::class.java.name)

    /**
     * Calls `InstallationStore(storeDir).install(id) { staging -> produce(staging.directory) }` on the copy and returns the
     * installation. An exception from the copy is rethrown as it is.
     */
    fun install(storeDir: Path, id: String, produce: (staging: Path) -> Path): Path {
        val ignoreEvents = Proxy.newProxyInstance(loader, arrayOf(listenerClass)) { _, method, _ ->
            when (method.name) {
                "hashCode" -> 0
                "equals" -> false
                "toString" -> "the other copy's listener"
                else -> null
            }
        }
        val store = storeClass
            .getConstructor(Path::class.java, String::class.java, leftoverPolicyClass, Long::class.javaObjectType, listenerClass)
            .newInstance(storeDir, ".provisioned", leftoverPolicyClass.getField("REPLACE").get(null), null, ignoreEvents)
        val produceInCopy: (Any) -> Path = { staging -> produce(stagingAreaClass.getMethod("getDirectory").invoke(staging) as Path) }
        return try {
            storeClass.getMethod("install", String::class.java, Function1::class.java).invoke(store, id, produceInCopy) as Path
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    override fun close() {
        loader.close()
    }

    /** Loads the library's own classes itself, from [libraryClasses], and everything else from the test's class loader. */
    private class LibraryFirstClassLoader(libraryClasses: URL) :
        URLClassLoader(arrayOf(libraryClasses), OtherCopyOfTheLibrary::class.java.classLoader) {

        override fun loadClass(name: String, resolve: Boolean): Class<*> {
            if (!name.startsWith(LIBRARY_PACKAGE)) return super.loadClass(name, resolve)
            synchronized(getClassLoadingLock(name)) {
                val loaded = findLoadedClass(name) ?: findClass(name)
                if (resolve) resolveClass(loaded)
                return loaded
            }
        }

        private companion object {
            val LIBRARY_PACKAGE = InstallationStore::class.java.name.substringBeforeLast(".") + "."
        }
    }
}
