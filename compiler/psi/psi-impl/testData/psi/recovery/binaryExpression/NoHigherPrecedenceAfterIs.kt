// COMPILATION_ERRORS
// It's found during working on binary expression optimizing KT-77993

val x = when { value is Int value2 is String -> return "" } // `value2` should not be treated as infix function