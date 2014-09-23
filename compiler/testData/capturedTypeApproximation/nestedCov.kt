class Cov<out T>
class Inv<T>

fun foo<T>(a: Cov<Inv<T>>) {}

// T captures 'in Int'
// lower: Cov<Nothing>
// upper: Cov<Inv<in Int>>

// T captures 'out Int'
// lower: Cov<Nothing>
// upper: Cov<Inv<out Int>>
