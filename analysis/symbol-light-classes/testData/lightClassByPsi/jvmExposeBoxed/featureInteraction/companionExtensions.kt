// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +CompanionBlocks +CompanionExtensions
@file:OptIn(ExperimentalStdlibApi::class)

class J

class K {
    companion {
        @get:JvmExposeBoxed("getRenamed")
        val renamed: UInt = 1u

        @get:JvmExposeBoxed
        val value: UInt = 2u

        @JvmExposeBoxed
        fun function(value: UInt = 3u): UInt = value
    }
}

@get:JvmExposeBoxed("KExtensionValue")
companion val K.extensionValue: UInt = 4u

@JvmExposeBoxed
companion fun K.extensionFunction(value: UInt = 5u): UInt = value

@get:JvmExposeBoxed("JExtensionValue")
companion val J.jExtensionValue: UInt = 6u

@JvmExposeBoxed
companion fun J.jExtensionFunction(value: UInt = 7u): UInt = value

// LIGHT_ELEMENTS_NO_DECLARATION: CompanionExtensionsKt.class[JExtensionValue;KExtensionValue;extensionFunction-WZ4Q5Ns;jExtensionFunction-WZ4Q5Ns], K.class[function-IKrLr70;getRenamed-pVg5ArA;getValue-pVg5ArA]
