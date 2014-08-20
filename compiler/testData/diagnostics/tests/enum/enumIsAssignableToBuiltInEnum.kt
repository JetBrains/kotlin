import java.lang.annotation.RetentionPolicy

enum class E {
    ENTRY
}

// Test resolve from source
val a: Enum<E> = E.ENTRY

// Test Java resolve
val b: Enum<RetentionPolicy> = RetentionPolicy.RUNTIME

// Test deserialized resolve
val c: Enum<InlineOption> = InlineOption.ONLY_LOCAL_RETURN
