Compilation of Native-specific IR into `.klib` files.

Code in this module does not depend on `:kotlin-native:backend.native` (which depends on heavy LLVM), which allows to use it
during lightweight first compilation phase: from `.kt` sources and klibs (including CInterop ones) to `.klib` library.
