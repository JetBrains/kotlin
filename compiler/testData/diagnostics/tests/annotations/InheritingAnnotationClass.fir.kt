// RUN_PIPELINE_TILL: FRONTEND
annotation class AnnKlass

class Child : <!FINAL_SUPERTYPE!>AnnKlass<!>
