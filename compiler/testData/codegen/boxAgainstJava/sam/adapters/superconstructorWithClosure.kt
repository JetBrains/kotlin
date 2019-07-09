// FILE: JavaClass.java

class JavaClass {
    JavaClass(Runnable r) {
        if (r != null) r.run();
    }
}

// FILE: 1.kt

var status: String = "fail"  // global property to avoid issues with accessing closure from local class (KT-4174)

internal class KotlinClass(): JavaClass({status="OK"}) {
}

fun box(): String {
    KotlinClass()
    return status
}
