// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE

// FILE: JavaFace.java
public interface JavaFace<T> {
    void singleMethod();
}

// FILE: JavaFaceUser.java
public class JavaFaceUser<T> {
    public <X> void use1(JavaFace<X> face) {}
    public void use2(JavaFace<T> face) {}
}

// FILE: KotlinSamUser.kt
fun JavaFaceUser<out Any>.useOut() {
    use1<Any> {}
    use2 {}
}

fun JavaFaceUser<in Any>.useIn() {
    use1<Any> {}
    use2 {}
}

fun JavaFaceUser<Any>.useInv() {
    use1<Any> {}
    use2 {}
}