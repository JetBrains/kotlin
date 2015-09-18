package p

interface I1
interface I2
interface I3

interface KotlinTrait<T1, T2>

class KotlinInheritor<T> : KotlinTrait<T, T>

// ALLOW_AST_ACCESS
