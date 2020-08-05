package org.jetbrains.konan.documentation

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.intellij.util.Function
import com.jetbrains.cidr.documentation.CocoaDocumentationManagerImpl
import com.jetbrains.cidr.documentation.CocoaDocumentationManagerImpl.DocTokenType.*
import com.jetbrains.cidr.documentation.XcodeDocumentationCandidateInfo
import com.jetbrains.cidr.documentation.XcodeDocumentationProvider
import org.jetbrains.kotlin.asJava.finder.KtLightPackage
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.util.*

// TODO: doc for modules, correct mapping of some parent class names from api notes (e.x. Swift specific bool getters)
class KonanXcodeDocumentationProvider : XcodeDocumentationProvider() {
  private companion object {
    const val META_SUFFIX = "Meta"
    const val PROTOCOL_SUFFIX = "Protocol"
    val NON_CAPITALIZABLE_PROPERTY = Regex("^[A-Z0-9]{2,}+")
  }

  override fun <T : Any?> doWithTokenInfo(
    element: PsiElement?,
    originalElement: PsiElement?,
    `fun`: Function<Info, T>, falseVal: T
  ): T {
    ApplicationManager.getApplication().assertReadAccessAllowed()

    // Some Xcode docs clash with Kotlin stdlib names
    if (element == null || (element as? KtElement)?.let { KtPsiUtil.getPackageName(it) } == "kotlin") {
      return falseVal
    }

    when (element) {
      is KtSimpleNameExpression -> { // workaround, until resolve will work correctly
        val referenceText = element.references.firstIsInstanceOrNull<KtSimpleNameReference>()?.canonicalText ?: element.text
        if (referenceText != null) {
          return `fun`.`fun`(
            InfoBuilder.createWithoutContainer(referenceText.getObjcClassName())
              .addTokenTypes(FUNCTION, CLASS, STRUCT, INSTANCE_PROPERTY, PROTOCOL, TYPEDEF, MACRO, ENUM, MODULE)
              .buildInfo()
          )
        }
      }
      is KtEnumEntry -> {
        val name = element.name
        if (name != null) {
          return `fun`.`fun`(
            InfoBuilder.create(name)
              .addContainers(element.containingClass()?.getObjcName())
              .addTokenTypes(ENUM_CASE, MACRO)
              .buildInfo()
          )
        }
      }
      is KtLightPackage -> {
        // TODO: JavaDocumentationProvider messes things up.
        // It fetches external documentation instead of us
        // val packageName = (element as? PsiPackageBase)?.name
        // if (packageName != null) {
        //   return `fun`.`fun`(Info(packageName, null, MODULE))
        // }
      }
      is KtClass -> {
        val className = element.getObjcName()
        if (className != null) {
          return `fun`.`fun`(
            InfoBuilder.createWithoutContainer(className)
              .addTokenTypes(CLASS, STRUCT, ENUM, PROTOCOL)
              .buildInfo()
          )
        }
      }
      is KtProperty -> {
        val propertyName = element.name
        if (propertyName != null) {
          var parentName = element.getParentOrReceiverClassName()

          // some enumeration cases imported as global props
          // but information about enclosing class is mangled in the package name
          if (element.isTopLevel && parentName == null) {
            val typeName = element.typeReference?.getTypeName()
            val typePackageName = KtPsiUtil.getPackageName(element)
            if (typePackageName != null) {
              parentName = typeName?.removePrefix(typePackageName)?.removeSuffix(propertyName)
            }
          }

          return `fun`.`fun`(
            InfoBuilder.createWithoutContainer(propertyName)
              .addPossibleContainerNames(parentName, element)
              .addTokenTypes(MACRO, TYPEDEF, ENUM_CASE, GLOBAL_VARIABLE, INSTANCE_PROPERTY, CLASS_PROPERTY)
              .buildInfo()
          )
        }
      }
      is KtTypeAlias -> {
        // Aliases produce additional entity with 'Var' suffix
        val propertyName = element.name?.removeSuffix("Var")
        if (propertyName != null) {
          return `fun`.`fun`(
            InfoBuilder.createWithoutContainer(propertyName)
              .addTokenTypes(TYPEDEF, MACRO, ENUM, STRUCT)
              .buildInfo()
          )
        }
      }
      is KtNamedFunction -> {
        val originalFunctionName = element.name
        if (originalFunctionName != null) {
          var functionName = originalFunctionName
          val hasContainer = element.receiverTypeReference != null || element.containingClass() != null

          if (hasContainer) { // if this function is converted from ObjC method
            functionName = buildObjcMethodName(originalFunctionName, element.valueParameters)

            // Generated ObjC property getters/setters are exported as separate functions.
            // If we succeeded to extract property name, then we more likely are
            // dealing with property accessor.
            val possiblePropertyName = buildObjcPropertyName(originalFunctionName, element)
            if (possiblePropertyName != null) {
              val result = `fun`.`fun`(
                InfoBuilder.create(possiblePropertyName)
                  .addPossibleContainerNames(element.getParentOrReceiverClassName(), element)
                  .addTokenTypes(INSTANCE_PROPERTY, CLASS_PROPERTY)
                  .buildInfo()
              )
              if (result != falseVal) {
                return result
              }
            }
          } else { // plain C function
            return `fun`.`fun`(
              InfoBuilder.createWithoutContainer(functionName)
                .addTokenTypes(FUNCTION)
                .buildInfo()
            )
          }

          return `fun`.`fun`(
            InfoBuilder.create(functionName)
              .addPossibleContainerNames(element.getParentOrReceiverClassName(), element)
              .addTokenTypes(INSTANCE_METHOD, CLASS_METHOD, INSTANCE_PROPERTY, CLASS_PROPERTY) // TODO: add InstanceVariable
              .buildInfo()
          )
        }
      }
      is KtObjectDeclaration -> {
        val containingClass = (element as KtElement).containingClass()
        if (containingClass != null) {
          return doWithTokenInfo(containingClass, originalElement, `fun`, falseVal)
        }
      }
      is KtConstructor<*> -> {
        val constructorDescriptor = element.descriptor as? ClassConstructorDescriptor
        val containingClass = (element as KtElement).containingClass()
        val classType = containingClass?.resolveToDescriptorIfAny()?.defaultType
        if (classType != null && constructorDescriptor != null) {
          val functionPsiElement = findInitKtMethodForConstructor(classType, constructorDescriptor)
          if (functionPsiElement != null) {
            // lookup corresponding ObjC initializer doc
            val initializerName = functionPsiElement.name?.let {
              buildObjcMethodName(it, functionPsiElement.valueParameters)
            }

            if (initializerName != null) {
              // We can't build correct Swift name, that's why we don't search for `LanguageEntityType::Initializer`
              return `fun`.`fun`(
                InfoBuilder.create(initializerName)
                  .addPossibleContainerNames(element.getParentOrReceiverClassName(), element)
                  .addTokenTypes(INSTANCE_METHOD)
                  .buildInfo()
              )
            }
          }
          return doWithTokenInfo(containingClass, originalElement, `fun`, falseVal)
        }
      }
    }
    return falseVal
  }

  private fun findInitKtMethodForConstructor(
    classType: SimpleType,
    constructorDescriptor: ClassConstructorDescriptor
  ): KtNamedFunction? {
    val memberScope = TypeUtils.getClassDescriptor(classType)?.unsubstitutedMemberScope

    // find initializer method with same parameter types
    val correspondingInitializer = memberScope?.getContributedDescriptors(DescriptorKindFilter.FUNCTIONS)?.find {
      it.name.asString().startsWith("init")
          && it is SimpleFunctionDescriptor
          && it.valueParameters.map { it.type } == constructorDescriptor.valueParameters.map { it.type }
    }
    return correspondingInitializer?.findPsi() as? KtNamedFunction
  }

  private fun buildObjcPropertyName(functionName: String, functionElement: KtNamedFunction): String? {
    val parameters = functionElement.valueParameters

    // possible property setter
    if (parameters.size == 1) {
      return functionName.convertToPropertyName("set")
    }

    // possible property getter
    if (parameters.isEmpty()) {
      return functionName.convertToPropertyName("get")
    }
    return null
  }

  /**
   * If `String` instance has [accessorPrefix],
   * returns converted property name, otherwise null.
   */
  private fun String.convertToPropertyName(accessorPrefix: String): String? {
    var result: String? = null
    if (startsWith(accessorPrefix) && this != accessorPrefix) {
      result = replaceFirst(accessorPrefix, "")
      if (!result.contains(NON_CAPITALIZABLE_PROPERTY)) { // e.x. NSMutableURLRequest::setHTTPShouldUsePipelining
        result = result.decapitalize()
      }
    }
    return result
  }

  private fun buildObjcMethodName(functionName: String, parameters: List<KtParameter>): String {
    if (parameters.isNotEmpty()) {
      val selectors = mutableListOf("$functionName:")
      // usually first selector is included
      // into Kotlin method signature
      selectors.addAll(parameters.drop(1).map { "${it.name!!}:" })
      return selectors.joinToString(separator = "")
    }
    return functionName
  }

  private fun KtClass.getObjcName(): String? {
    return name?.getObjcClassName()
  }

  private fun KtCallableDeclaration.getParentOrReceiverClassName(): String? {
    val receiverClassTypeName = receiverTypeReference?.getTypeName()?.getObjcClassName()
    return containingClass()?.getObjcName() ?: receiverClassTypeName
  }

  // TODO: collect bridges Swift parent names too, or fix them in [XcodeJsonDocSearch]
  private fun InfoBuilder.addPossibleContainerNames(
    parentClassName: String?,
    element: KtCallableDeclaration
  ): InfoBuilder {
    addContainers(parentClassName)
    // Manually add NSObject because it isn't resolved correctly for now
    addContainers("NSObject")

    val allSuperTypeNames = element.containingClass()?.getAllSuperTypeNames()
    if (allSuperTypeNames != null) {
      addContainers(allSuperTypeNames)
    }
    return this
  }

  private fun KtClass.getAllSuperTypeNames(): List<String>? {
    val classType = resolveToDescriptorIfAny()?.typeConstructor?.declarationDescriptor?.defaultType
    return classType?.let {
      TypeUtils.getAllSupertypes(classType).mapNotNull { type ->
        if (!KotlinBuiltIns.isAny(type)) {
          TypeUtils.getClassDescriptor(type)?.name?.identifier?.getObjcClassName()
        } else {
          null
        }
      }
    }
  }

  private fun KtTypeReference.getTypeName() =
    (typeElement as? KtUserType)?.referenceExpression?.getReferencedName()

  // ObjC protocols are converted to Kotlin classes with 'Protocol' verb attached.
  // Type methods are converted to Kotlin classes with 'Meta' verb attached.
  // see in kotlin-native: org.jetbrains.kotlin.native.interop.gen.ObjCClassOrProtocol.kotlinClassName
  private fun String.getObjcClassName(): String {
    var result = this
    if (result != META_SUFFIX) {
      result = result.removeSuffix(META_SUFFIX)
    }

    if (result != PROTOCOL_SUFFIX) {
      result = result.removeSuffix(PROTOCOL_SUFFIX)
    }
    return result
  }

  // TODO: move this builder to AppCodeDocumentationProvider after K/N to master branch merge
  private class InfoBuilder private constructor(private var myDocElementName: String) {
    private val myDocTokenTypes: MutableSet<CocoaDocumentationManagerImpl.DocTokenType>
    private val myContainers: MutableSet<String?>

    init {
      myDocTokenTypes = EnumSet.noneOf(CocoaDocumentationManagerImpl.DocTokenType::class.java)
      myContainers = HashSet()
    }

    fun addTokenTypes(vararg types: CocoaDocumentationManagerImpl.DocTokenType): InfoBuilder {
      myDocTokenTypes.addAll(types)
      return this
    }

    fun addContainers(vararg containers: String?): InfoBuilder {
      myContainers.addAll(containers)
      return this
    }

    fun addContainers(containers: List<String>): InfoBuilder {
      myContainers.addAll(containers)
      return this
    }

    fun buildInfo(): Info {
      val candidates = ArrayList<XcodeDocumentationCandidateInfo>()
      for (docTokenType in myDocTokenTypes) {
        for (container in myContainers) {
          candidates.add(XcodeDocumentationCandidateInfo.create(container, docTokenType))
        }
      }
      return Info(myDocElementName, candidates)
    }

    companion object {
      fun create(docElementName: String): InfoBuilder {
        return InfoBuilder(docElementName)
      }

      fun createWithoutContainer(docElementName: String): InfoBuilder {
        return InfoBuilder(docElementName).addContainers(null)
      }
    }
  }
}