class C<V>() {
    <error descr="[INAPPLICABLE_LATEINIT_MODIFIER] 'lateinit' modifier is not allowed on properties of a type with nullable upper bound">lateinit</error> var item: V
}