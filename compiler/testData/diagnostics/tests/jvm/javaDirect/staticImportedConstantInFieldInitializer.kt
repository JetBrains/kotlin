// RUN_PIPELINE_TILL: BACKEND
// FILE: a/Constants.java
package a;

public class Constants {
    public static final int MAX = 100;
}

// FILE: b/Holder.java
package b;

import static a.Constants.MAX;

public class Holder {
    public static final int LIMIT = MAX;
}

// FILE: main.kt
package b

annotation class Ann(val value: Int)

@Ann(Holder.LIMIT)
fun test() {}

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, javaProperty, primaryConstructor, propertyDeclaration */
