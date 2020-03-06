enum class A {
    ONE {
        override fun toString(): String = "1"
    },
    TWO,
    THREE {
        override fun toString(): String = "3"
    }
}

// There is always one CHECKCAST in the `valueOf` method, since this calls a library function which returns an `Enum`.
// 1 CHECKCAST A

// The JVM backend produces one additional CHECKCAST instruction in `values`
// JVM_TEMPLATES:
// 1 CHECKCAST \[LA;
// 2 CHECKCAST

// The JVM_IR backend does not use clone to copy the `$VALUES` array, so there is no CHECKCAST in `values`.
// JVM_IR_TEMPLATES:
// 1 CHECKCAST
