fun box(): String {
    if ((1 as? Int) == null) return "fail 1"
    if ((1 as? Byte) != null) return "fail 2"
    if ((1 as? Short) != null) return "fail 3"
    if ((1 as? Long) != null) return "fail 4"
    if ((1 as? Char) != null) return "fail 5"
    if ((1 as? Double) != null) return "fail 6"
    if ((1 as? Float) != null) return "fail 7"

    if ((1.0 as? Int) != null) return "fail 11"
    if ((1.0 as? Byte) != null) return "fail 12"
    if ((1.0 as? Short) != null) return "fail 13"
    if ((1.0 as? Long) != null) return "fail 14"
    if ((1.0 as? Char) != null) return "fail 15"
    if ((1.0 as? Double) == null) return "fail 16"
    if ((1.0 as? Float) != null) return "fail 17"

    if ((1f as? Int) != null) return "fail 21"
    if ((1f as? Byte) != null) return "fail 22"
    if ((1f as? Short) != null) return "fail 23"
    if ((1f as? Long) != null) return "fail 24"
    if ((1f as? Char) != null) return "fail 25"
    if ((1f as? Double) != null) return "fail 26"
    if ((1f as? Float) == null) return "fail 27"

    return "OK"
}
