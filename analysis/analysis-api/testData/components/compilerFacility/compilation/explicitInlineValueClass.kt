// LANGUAGE: +FullValueClasses

inline value class ExplicitInlineValueClass(val value: Int) {
    fun plus(other: ExplicitInlineValueClass): ExplicitInlineValueClass = ExplicitInlineValueClass(value + other.value)
}

value class FullValueClass(val value: Int)

fun create(value: Int): ExplicitInlineValueClass = ExplicitInlineValueClass(value)

fun convert(value: ExplicitInlineValueClass): FullValueClass = FullValueClass(value.value)
