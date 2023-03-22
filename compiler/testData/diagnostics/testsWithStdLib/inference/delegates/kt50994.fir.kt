// IGNORE_REVERSED_RESOLVE
// FIR_DUMP
// WITH_REFLECT

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty1

class ProcessorWithParent : Entity {
    var processor by parent(ProcessorWithChildren::processors)
}

class ProcessorWithChildren : Entity {
    var processors by <!INAPPLICABLE_CANDIDATE!>children<!>(ProcessorWithParent::class.java, ProcessorWithParent::<!UNRESOLVED_REFERENCE!>processor<!>)
}

class Processor2WithParent : Entity {
    var processor: Processor2WithChildren? by parent(Processor2WithChildren::processors)
}

class Processor2WithChildren : Entity {
    var processors by children(Processor2WithParent::class.java, Processor2WithParent::processor)
}

class Processor3WithParent : Entity {
    var processor by parent(Processor3WithChildren::processors)
}

class Processor3WithChildren : Entity {
    var processors: MutableCollection<Processor3WithParent> by children(Processor3WithParent::class.java, Processor3WithParent::processor)
}

inline fun <reified SP : Entity, reified TP : Entity> SP.parent(
    property: KProperty1<TP, MutableCollection<SP>>
): Delegate<SP, TP?> = null!!

fun <SC : Entity, TC : Entity> SC.children(
    clazz: Class<TC>, property: KProperty1<TC, SC?>, name: String = property.name
): Delegate<SC, MutableCollection<TC>> = null!!

interface Delegate<R : Entity, T> : ReadWriteProperty<R, T>

interface Entity
