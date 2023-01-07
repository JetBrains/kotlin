// TARGET_BACKEND: JVM_IR

// WITH_STDLIB

import kotlin.Self

open class Person(builder: PersonBuilder<*>) {
    var name: String

    init {
        this.name = builder.name
    }
}

class Employee(builder: EmployeeBuilder) : Person(builder) {
    var role: String

    init {
        this.role = builder.role
    }
}

@Self
open class PersonBuilder {
    lateinit var name: String

    fun setName(name: String): Self {
        this.name = name
        return this as Self
    }

    open fun build(): Person = Person(this)
}

class EmployeeBuilder : PersonBuilder<EmployeeBuilder>() {
    lateinit var role: String

    fun setRole(role: String): EmployeeBuilder {
        this.role = role
        return this
    }

    override fun build(): Employee = Employee(this)
}

fun box(): String {
    val person: Person = PersonBuilder().setName("Person").build()
    val employee: Employee = EmployeeBuilder().setName("Employee").setRole("Role").build()
    val predicate = (person.name == "Person" && employee.name == "Employee" && employee.role == "Role")
    return if (predicate) "OK" else "ERROR"
}