// RUN_PIPELINE_TILL: SOURCE
annotation class AnnKlass

class Child : <!FINAL_SUPERTYPE!>AnnKlass<!>
