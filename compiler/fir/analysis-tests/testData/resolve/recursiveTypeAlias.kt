interface Something<D, T : () -> Something1<D>>
typealias Something1<D> = <!RECURSIVE_TYPEALIAS_EXPANSION!>Something<D, () -> Something1<D>><!>
