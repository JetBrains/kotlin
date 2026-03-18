// RENDER_PSI_CLASS_NAME
// FILE: JavaAnnotation.java

public @interface JavaAnnotation {
    String value();
}

// FILE: main.kt

@JavaAnnotation(<expr>"str"</expr>)
fun usage() {

}
