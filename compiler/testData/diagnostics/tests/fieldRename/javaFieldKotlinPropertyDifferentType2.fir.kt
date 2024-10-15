// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-57905

// FILE: BasicSliderUI.java
public class BasicSliderUI {
    Rectangle thumbRect = null;
}

// FILE: Main.kt

class Rectangle

class TimelineSliderUI: BasicSliderUI() {
    // K1: ok
    // K2: INITIALIZER_TYPE_MISMATCH (actual kotlin/Function0<kotlin/Function0<Rectangle>>, expected kotlin/Function0<Rectangle>)
    val <!PROPERTY_HIDES_JAVA_FIELD!>thumbRect<!>: () -> Rectangle = <!INITIALIZER_TYPE_MISMATCH!>{ thumbRect }<!>
}
