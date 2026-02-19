// COMPILATION_ERRORS
// It's found during working on binary expression optimizing KT-77993

val x = @Ann() class C annotation y // `annotation` should be parsed as infix function