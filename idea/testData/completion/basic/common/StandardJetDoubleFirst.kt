fun some(a: Double<caret>) {
}

// INVOCATION_COUNT: 2
// WITH_ORDER: 1
// EXIST: { lookupString:"Double", tailText:" (jet)" }
// EXIST_JAVA_ONLY: { lookupString:"Double", tailText:" (java.lang)" }
// EXIST: { lookupString:"DoubleArray", tailText:" (jet)" }
