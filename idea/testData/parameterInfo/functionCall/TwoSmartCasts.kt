// See KT-14484

class C {
    fun isEqualKey(key1: Any?, key2: Any?): Boolean {
        if (key1 is String && key2 is String) {
            return key1.equals(key2,  <caret>)
        }
    }
}
/*
Text: (other: Any?), Disabled: true, Strikeout: false, Green: true
Text: (other: String?, <highlight>ignoreCase: Boolean = false</highlight>), Disabled: false, Strikeout: false, Green: false
*/