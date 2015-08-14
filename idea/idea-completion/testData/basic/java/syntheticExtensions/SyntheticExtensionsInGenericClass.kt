fun foo(klass: Class<*>) {
    klass.<caret>
}

// EXIST: simpleName
// ABSENT: getSimpleName
// EXIST: enclosingClass
// ABSENT: getEnclosingClass
// EXIST: annotations
// ABSENT: getAnnotations
