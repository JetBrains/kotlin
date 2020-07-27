import org.jetbrains.annotations.PropertyKey

fun message(@PropertyKey(resourceBundle = "PropertyKeysWithPrefix") key: String) = key

fun test() {
    message("foo.<caret>")
}

// EXIST: { lookupString: "foo.bar", itemText: "foo.bar", tailText: "1", typeText: "PropertyKeysWithPrefix" }
// EXIST: { lookupString: "foo.bar.baz", itemText: "foo.bar.baz", tailText: "3", typeText: "PropertyKeysWithPrefix" }
// EXIST: { lookupString: "foo.test", itemText: "foo.test", tailText: "4", typeText: "PropertyKeysWithPrefix" }
// NOTHING_ELSE