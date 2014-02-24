import inlibrary.test.*

val a: <caret>ReferenceTest? = null

// CONTEXT: val test: Test? = null
// WITH_LIBRARY: /resolve/referenceInLib/inLibrarySource

// REF: (inlibrary.test).Test