class TestClass {
    <caret>
    object O {}
}

// EXIST: { itemText: "companion object", tailText: " {...}" }
