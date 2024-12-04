import org.jetbrains.kotlin.buildtools.api.CompilationService
import org.jetbrains.kotlin.buildtools.api.SharedApiClassesClassLoader
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

class SharedApiClassesClassLoaderTest {
    @DisplayName("No application classes are leaked into the `SharedApiClassesClassLoader`")
    @Test
    fun testNoLeakedApplicationClasses() {
        val possibleLeakedClass = "org.junit.jupiter.api.Test"
        ClassLoader.getSystemClassLoader()
            .loadClass(possibleLeakedClass) // Ensure that the class is there. Otherwise, ClassNotFoundException will be thrown.
        val sharedApiClassesClassLoader = SharedApiClassesClassLoader()
        assertThrows<ClassNotFoundException>("`$possibleLeakedClass` is present in the system classloader, but must not be present in the custom classloader") {
            sharedApiClassesClassLoader.loadClass(possibleLeakedClass)
        }
        sharedApiClassesClassLoader.loadClass(CompilationService::class.java.name) // The build tools API classes are still available
    }
}