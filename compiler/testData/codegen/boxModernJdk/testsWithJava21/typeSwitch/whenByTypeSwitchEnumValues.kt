// TARGET_BACKEND: JVM
// WHEN_EXPRESSIONS: INDY

// CHECK_BYTECODE_TEXT
// 1 INVOKEDYNAMIC typeSwitch
// 0 INSTANCEOF

// TODO: (it actually works with WhenMappings.class) It has worked before this way already
// But it is a good place to have this test
