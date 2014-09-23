class Cov<out T>

fun foo<T>(a: Cov<T>) {}

// T captures 'in Int'
// lower: Cov<Int>
// upper: Cov<Any?>

// T captures 'out Int'
// lower: Cov<Nothing>
// upper: Cov<Int>
