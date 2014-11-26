package p

trait I1
trait I2
trait I3

trait KotlinTrait<T1, T2>

class KotlinInheritor<T> : KotlinTrait<T, T>

// ALLOW_AST_ACCESS
