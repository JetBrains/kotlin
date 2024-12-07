// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-70327
// MODULE: main
// FILE: JavaA.java
public class JavaA<T extends JavaB> {

}

// FILE: JavaALeaf.java
public class JavaALeaf extends JavaA<JavaBLeaf> {

}

// FILE: JavaB.java
public class JavaB<T extends JavaA> {

}

// FILE: JavaBLeaf.java
public class JavaBLeaf extends JavaB<JavaALeaf> {

}

// FILE: main.kt
class KotlinA : JavaA<JavaB<JavaALeaf>>()

class KotlinB : JavaB<JavaA<JavaBLeaf>>()
