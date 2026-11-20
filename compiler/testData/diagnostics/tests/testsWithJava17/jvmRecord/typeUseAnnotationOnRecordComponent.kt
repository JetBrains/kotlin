// ENABLE_FOREIGN_ANNOTATIONS
// WITH_STDLIB
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89934
// JSPECIFY_STATE: strict

// FILE: ImplicitRecord.java
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

record ImplicitRecord(@Nullable String nullable, @NonNull String required) {}

// FILE: CompactRecord.java
import org.jspecify.annotations.NonNull;

record CompactRecord(@NonNull String required) {
    CompactRecord {}
}

// FILE: ExplicitCanonicalRecord.java
import org.jspecify.annotations.NonNull;

record ExplicitCanonicalRecord(@NonNull String required) {
    ExplicitCanonicalRecord(String required) {
        this.required = required;
    }
}

// FILE: ExplicitAccessorRecord.java
import org.jspecify.annotations.Nullable;

record ExplicitAccessorRecord(@Nullable String nullable) {
    public String nullable() {
        return nullable;
    }
}

// FILE: ArrayRecord.java
import org.jspecify.annotations.Nullable;

record ArrayRecord(@Nullable String[] values) {}

// FILE: VarargRecord.java
import org.jspecify.annotations.Nullable;

record VarargRecord(@Nullable String... values) {}

// FILE: test.kt
private fun implicitRecord(record: ImplicitRecord, value: String?) {
    record.nullable()<!UNSAFE_CALL!>.<!>length
    ImplicitRecord(null, <!ARGUMENT_TYPE_MISMATCH!>value<!>)
}

private fun compactRecord(value: String?) {
    CompactRecord(<!ARGUMENT_TYPE_MISMATCH!>value<!>)
}

private fun explicitCanonicalRecord(value: String?) {
    ExplicitCanonicalRecord(value)
}

private fun explicitAccessor(record: ExplicitAccessorRecord) {
    record.nullable().length
}

private fun arrayRecord(record: ArrayRecord, varargRecord: VarargRecord) {
    record.values()[0]<!UNSAFE_CALL!>.<!>length
    varargRecord.values()[0]<!UNSAFE_CALL!>.<!>length
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, nullableType */
