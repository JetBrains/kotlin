// FILE: main.kt
/**
 * [Storage.<caret_1>value]
 * [Storage2.<caret_2>value2]
 */
fun usage() {

}

// FILE: Storage.java
@interface Storage {
    String value() default "";
}

// FILE: Storage2.java
@interface Storage2 {
    String value2();
}
