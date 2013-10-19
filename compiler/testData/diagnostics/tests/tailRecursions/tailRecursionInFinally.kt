tailRecursive fun test() : Unit {
    try {
    } finally {
        <!PARTIAL_TAIL_RECURSIVE_CALL!>test<!>()
    }
}