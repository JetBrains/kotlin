package templates

import kotlin.coroutines.experimental.buildSequence
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf


typealias TemplateGroup = () -> Sequence<MemberTemplate>

fun templateGroupOf(vararg templates: MemberTemplate): TemplateGroup = { templates.asSequence() }

abstract class TemplateGroupBase : TemplateGroup {

    override fun invoke(): Sequence<MemberTemplate> = buildSequence {
        with(this@TemplateGroupBase) {
            this::class.members.filter { it.name.startsWith("f_") }.forEach {
                    require(it.parameters.size == 1) { "Member $it violates naming convention" }
                    when {
                        it.returnType.isSubtypeOf(typeMemberTemplate) ->
                            yield(it.call(this) as MemberTemplate)
                        it.returnType.isSubtypeOf(typeIterableOfMemberTemplates) ->
                            @Suppress("UNCHECKED_CAST")
                            yieldAll(it.call(this) as Iterable<MemberTemplate>)
                        else ->
                            error("Member $it violates naming convention")
                    }
                }
        }
    }.run {
        if (defaultActions.isEmpty()) this else onEach { t -> defaultActions.forEach(t::builder) }
    }

    private val defaultActions = mutableListOf<MemberBuildAction>()

    fun defaultBuilder(builderAction: MemberBuildAction) {
        defaultActions += builderAction
    }

    companion object {
        private val typeMemberTemplate = MemberTemplate::class.createType()
        private val typeIterableOfMemberTemplates = Iterable::class.createType(arguments = listOf(KTypeProjection.invariant(typeMemberTemplate)))
    }

}

fun foo() {
    cacheFile.objectInputStream().use {
        // one indent here
    }
}
