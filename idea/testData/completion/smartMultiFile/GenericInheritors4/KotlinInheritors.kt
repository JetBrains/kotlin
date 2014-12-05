package p

trait I1<T>
trait I2

trait KotlinTrait<T>

class KotlinInheritor<T> : KotlinTrait<I1<T>>

// ALLOW_AST_ACCESS
