fun foo(klass: Class<*>) {
    klass.<caret>
}

// EXIST_JAVA_ONLY: simpleName
// ABSENT: getSimpleName
// EXIST_JAVA_ONLY: enclosingClass
// ABSENT: getEnclosingClass
// EXIST_JAVA_ONLY: annotations
// ABSENT: getAnnotations
