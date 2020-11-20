// NOTE: test should fail when https://github.com/gradle/gradle/issues/13783 is fixed
// and the related workaround in SourceNavigationHelper is removed
// Then test can be removed
test.KObject.<caret>foo()

// DEPENDENCIES: classpath:module-classes

// REF: (in test.KObject).foo()
// FILE: test/custom.kt