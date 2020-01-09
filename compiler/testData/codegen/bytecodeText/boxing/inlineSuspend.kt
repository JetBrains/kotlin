// !LANGUAGE: +ReleaseCoroutines

inline suspend fun inlineMe() = 1000

// inlineMe$$forInline : valueOf
// inlineMe : boxInt

// 1 valueOf
// 1 boxInt
