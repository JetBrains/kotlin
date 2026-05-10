// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +EnhancementsOfSecondIncorporationKind25
// ISSUE: KT-83068

sealed class PhysicalQuantity {
    sealed class PhysicalQuantityWithDimension : PhysicalQuantity()

    data class Undefined<CustomQuantity : UndefinedQuantityType>(val customQuantity: CustomQuantity) : PhysicalQuantityWithDimension()
}

sealed class UndefinedQuantityType {
    data class Dividing<NumeratorQuantity : UndefinedQuantityType, DenominatorQuantity : UndefinedQuantityType>(
        val numerator: NumeratorQuantity,
        val denominator: DenominatorQuantity,
    ) : UndefinedQuantityType()

    data class Multiplying<Left : UndefinedQuantityType, Right : UndefinedQuantityType>(val left: Left, val right: Right) : UndefinedQuantityType()

    data class Reciprocal<Quantity : UndefinedQuantityType>(val reciprocal: Quantity) : UndefinedQuantityType()
}

sealed class AbstractUndefinedScientificUnit<QuantityType : UndefinedQuantityType> :
    AbstractScientificUnit<PhysicalQuantity.Undefined<QuantityType>>(),
    UndefinedScientificUnit<QuantityType>

sealed class AbstractScientificUnit<Quantity : PhysicalQuantity> : ScientificUnit<Quantity>

sealed interface UndefinedScientificUnit<QuantityType : UndefinedQuantityType> : ScientificUnit<PhysicalQuantity.Undefined<QuantityType>>

sealed interface ScientificUnit<Quantity : PhysicalQuantity>

sealed class UndefinedMultipliedUnit<
        LeftQuantity : UndefinedQuantityType,
        LeftUnit : AbstractUndefinedScientificUnit<LeftQuantity>,
        RightQuantity : UndefinedQuantityType,
        RightUnit : AbstractUndefinedScientificUnit<RightQuantity>,
        > : AbstractUndefinedScientificUnit<UndefinedQuantityType.Multiplying<LeftQuantity, RightQuantity>>() {
    abstract val left: LeftUnit
    abstract val right: RightUnit
}

sealed class UndefinedDividedUnit<
        NumeratorQuantity : UndefinedQuantityType,
        NumeratorUnit : AbstractUndefinedScientificUnit<NumeratorQuantity>,
        DenominatorQuantity : UndefinedQuantityType,
        DenominatorUnit : AbstractUndefinedScientificUnit<DenominatorQuantity>,
        > : AbstractUndefinedScientificUnit<UndefinedQuantityType.Dividing<NumeratorQuantity, DenominatorQuantity>>() {
    abstract val numerator: NumeratorUnit
    abstract val denominator: DenominatorUnit
}

sealed class UndefinedReciprocalUnit<
        InverseQuantity : UndefinedQuantityType,
        InverseUnit : AbstractUndefinedScientificUnit<InverseQuantity>,
        > :
    AbstractUndefinedScientificUnit<UndefinedQuantityType.Reciprocal<InverseQuantity>>() {
    abstract val inverse: InverseUnit
}

typealias UndefinedScientificValue<Quantity, Unit> = ScientificValue<PhysicalQuantity.Undefined<Quantity>, Unit>

interface ScientificValue<Quantity : PhysicalQuantity, Unit : ScientificUnit<Quantity>> :
    Comparable<ScientificValue<Quantity, *>> {
    val unit: Unit
}

sealed class Decimal : Comparable<Decimal>

internal fun <
        LeftNumeratorLeftAndRightNumeratorLeftQuantity : UndefinedQuantityType,
        LeftNumeratorLeftUnit : AbstractUndefinedScientificUnit<LeftNumeratorLeftAndRightNumeratorLeftQuantity>,
        LeftNumeratorRightQuantity : UndefinedQuantityType,
        LeftNumeratorRightUnit : AbstractUndefinedScientificUnit<LeftNumeratorRightQuantity>,
        LeftNumeratorUnit : UndefinedMultipliedUnit<
                LeftNumeratorLeftAndRightNumeratorLeftQuantity,
                LeftNumeratorLeftUnit,
                LeftNumeratorRightQuantity,
                LeftNumeratorRightUnit,
                >,
        LeftDenominatorAndRightDenominatorLeftQuantity : UndefinedQuantityType,
        LeftDenominatorUnit : AbstractUndefinedScientificUnit<LeftDenominatorAndRightDenominatorLeftQuantity>,
        LeftUnit : UndefinedDividedUnit<
                UndefinedQuantityType.Multiplying<
                        LeftNumeratorLeftAndRightNumeratorLeftQuantity,
                        LeftNumeratorRightQuantity,
                        >,
                LeftNumeratorUnit,
                LeftDenominatorAndRightDenominatorLeftQuantity,
                LeftDenominatorUnit,
                >,
        RightNumeratorLeftUnit : AbstractUndefinedScientificUnit<LeftNumeratorLeftAndRightNumeratorLeftQuantity>,
        RightNumeratorRightQuantity : UndefinedQuantityType,
        RightNumeratorRightUnit : AbstractUndefinedScientificUnit<RightNumeratorRightQuantity>,
        RightNumeratorUnit : UndefinedMultipliedUnit<
                LeftNumeratorLeftAndRightNumeratorLeftQuantity,
                RightNumeratorLeftUnit,
                RightNumeratorRightQuantity,
                RightNumeratorRightUnit,
                >,
        RightDenominatorLeftUnit : AbstractUndefinedScientificUnit<LeftDenominatorAndRightDenominatorLeftQuantity>,
        RightDenominatorRightQuantity : UndefinedQuantityType,
        RightDenominatorRightUnit : AbstractUndefinedScientificUnit<RightDenominatorRightQuantity>,
        RightDenominatorUnit : UndefinedMultipliedUnit<
                LeftDenominatorAndRightDenominatorLeftQuantity,
                RightDenominatorLeftUnit,
                RightDenominatorRightQuantity,
                RightDenominatorRightUnit,
                >,
        RightUnit : UndefinedDividedUnit<
                UndefinedQuantityType.Multiplying<
                        LeftNumeratorLeftAndRightNumeratorLeftQuantity,
                        RightNumeratorRightQuantity,
                        >,
                RightNumeratorUnit,
                UndefinedQuantityType.Multiplying<
                        LeftDenominatorAndRightDenominatorLeftQuantity,
                        RightDenominatorRightQuantity,
                        >,
                RightDenominatorUnit,
                >,
        TargetNumeratorUnit : UndefinedMultipliedUnit<
                UndefinedQuantityType.Multiplying<
                        LeftNumeratorLeftAndRightNumeratorLeftQuantity,
                        LeftNumeratorRightQuantity,
                        >,
                LeftNumeratorUnit,
                UndefinedQuantityType.Multiplying<
                        LeftNumeratorLeftAndRightNumeratorLeftQuantity,
                        RightNumeratorRightQuantity,
                        >,
                RightNumeratorUnit,
                >,
        TargetDenominatorLeftUnit : UndefinedMultipliedUnit<
                LeftDenominatorAndRightDenominatorLeftQuantity,
                LeftDenominatorUnit,
                LeftDenominatorAndRightDenominatorLeftQuantity,
                LeftDenominatorUnit,
                >,
        TargetDenominatorUnit : UndefinedMultipliedUnit<
                UndefinedQuantityType.Multiplying<
                        LeftDenominatorAndRightDenominatorLeftQuantity,
                        LeftDenominatorAndRightDenominatorLeftQuantity,
                        >,
                TargetDenominatorLeftUnit,
                RightDenominatorRightQuantity,
                RightDenominatorRightUnit,
                >,
        TargetUnit : UndefinedDividedUnit<
                UndefinedQuantityType.Multiplying<
                        UndefinedQuantityType.Multiplying<
                                LeftNumeratorLeftAndRightNumeratorLeftQuantity,
                                LeftNumeratorRightQuantity,
                                >,
                        UndefinedQuantityType.Multiplying<
                                LeftNumeratorLeftAndRightNumeratorLeftQuantity,
                                RightNumeratorRightQuantity,
                                >,
                        >,
                TargetNumeratorUnit,
                UndefinedQuantityType.Multiplying<
                        UndefinedQuantityType.Multiplying<
                                LeftDenominatorAndRightDenominatorLeftQuantity,
                                LeftDenominatorAndRightDenominatorLeftQuantity,
                                >,
                        RightDenominatorRightQuantity,
                        >,
                TargetDenominatorUnit,
                >,
        TargetValue : UndefinedScientificValue<
                UndefinedQuantityType.Dividing<
                        UndefinedQuantityType.Multiplying<
                                UndefinedQuantityType.Multiplying<
                                        LeftNumeratorLeftAndRightNumeratorLeftQuantity,
                                        LeftNumeratorRightQuantity,
                                        >,
                                UndefinedQuantityType.Multiplying<
                                        LeftNumeratorLeftAndRightNumeratorLeftQuantity,
                                        RightNumeratorRightQuantity,
                                        >,
                                >,
                        UndefinedQuantityType.Multiplying<
                                UndefinedQuantityType.Multiplying<
                                        LeftDenominatorAndRightDenominatorLeftQuantity,
                                        LeftDenominatorAndRightDenominatorLeftQuantity,
                                        >,
                                RightDenominatorRightQuantity,
                                >,
                        >,
                TargetUnit,
                >,
        > UndefinedScientificValue<
        UndefinedQuantityType.Dividing<
                UndefinedQuantityType.Multiplying<
                        LeftNumeratorLeftAndRightNumeratorLeftQuantity,
                        LeftNumeratorRightQuantity,
                        >,
                LeftDenominatorAndRightDenominatorLeftQuantity,
                >,
        LeftUnit,
        >.internalDividingWithMultiplyingNumeratorMultipliedByDividingUnitWithMultiplyingNumeratorWithNumeratorLeftAsLeftAndMultiplyingDenominatorWithDenominatorAsLeft(
    right: UndefinedScientificValue<
            UndefinedQuantityType.Dividing<
                    UndefinedQuantityType.Multiplying<
                            LeftNumeratorLeftAndRightNumeratorLeftQuantity,
                            RightNumeratorRightQuantity,
                            >,
                    UndefinedQuantityType.Multiplying<
                            LeftDenominatorAndRightDenominatorLeftQuantity,
                            RightDenominatorRightQuantity,
                            >,
                    >,
            RightUnit,
            >,
    leftNumeratorUnitXRightNumeratorUnit: LeftNumeratorUnit.(RightNumeratorUnit) -> TargetNumeratorUnit,
    leftDenominatorUnitXLeftDenominatorUnit: LeftDenominatorUnit.(LeftDenominatorUnit) -> TargetDenominatorLeftUnit,
    targetDenominatorLeftUnitXRightDenominatorRightUnit: TargetDenominatorLeftUnit.(RightDenominatorRightUnit) -> TargetDenominatorUnit,
    targetNumeratorUnitPerTargetDenominatorUnit: TargetNumeratorUnit.(TargetDenominatorUnit) -> TargetUnit,
    factory: (Decimal, TargetUnit) -> TargetValue,
) = unit.numerator.leftNumeratorUnitXRightNumeratorUnit(
    right.unit.numerator,
).targetNumeratorUnitPerTargetDenominatorUnit(
    unit.denominator.leftDenominatorUnitXLeftDenominatorUnit(
        unit.denominator,
    ).targetDenominatorLeftUnitXRightDenominatorRightUnit(
        right.unit.denominator.right,
    ),
).byMultiplying(this, right, factory)

fun <
        LeftNumeratorLeftAndRightNumeratorLeftQuantity : UndefinedQuantityType,
        LeftNumeratorLeftUnit : AbstractUndefinedScientificUnit<LeftNumeratorLeftAndRightNumeratorLeftQuantity>,
        LeftNumeratorRightQuantity : UndefinedQuantityType,
        LeftNumeratorRightUnit : AbstractUndefinedScientificUnit<LeftNumeratorRightQuantity>,
        LeftNumeratorUnit : UndefinedMultipliedUnit<
                LeftNumeratorLeftAndRightNumeratorLeftQuantity,
                LeftNumeratorLeftUnit,
                LeftNumeratorRightQuantity,
                LeftNumeratorRightUnit,
                >,
        LeftDenominatorAndRightDenominatorLeftQuantity : UndefinedQuantityType,
        LeftDenominatorUnit : AbstractUndefinedScientificUnit<LeftDenominatorAndRightDenominatorLeftQuantity>,
        LeftUnit : UndefinedDividedUnit<
                UndefinedQuantityType.Multiplying<
                        LeftNumeratorLeftAndRightNumeratorLeftQuantity,
                        LeftNumeratorRightQuantity,
                        >,
                LeftNumeratorUnit,
                LeftDenominatorAndRightDenominatorLeftQuantity,
                LeftDenominatorUnit,
                >,
        RightNumeratorLeftUnit : AbstractUndefinedScientificUnit<LeftNumeratorLeftAndRightNumeratorLeftQuantity>,
        RightNumeratorRightQuantity : UndefinedQuantityType,
        RightNumeratorRightUnit : AbstractUndefinedScientificUnit<RightNumeratorRightQuantity>,
        RightNumeratorUnit : UndefinedMultipliedUnit<
                LeftNumeratorLeftAndRightNumeratorLeftQuantity,
                RightNumeratorLeftUnit,
                RightNumeratorRightQuantity,
                RightNumeratorRightUnit,
                >,
        RightDenominatorLeftUnit : AbstractUndefinedScientificUnit<LeftDenominatorAndRightDenominatorLeftQuantity>,
        RightDenominatorRightQuantity : UndefinedQuantityType,
        RightDenominatorRightUnit : AbstractUndefinedScientificUnit<RightDenominatorRightQuantity>,
        RightDenominatorUnit : UndefinedMultipliedUnit<
                LeftDenominatorAndRightDenominatorLeftQuantity,
                RightDenominatorLeftUnit,
                RightDenominatorRightQuantity,
                RightDenominatorRightUnit,
                >,
        RightUnit : UndefinedDividedUnit<
                UndefinedQuantityType.Multiplying<
                        LeftNumeratorLeftAndRightNumeratorLeftQuantity,
                        RightNumeratorRightQuantity,
                        >,
                RightNumeratorUnit,
                UndefinedQuantityType.Multiplying<
                        LeftDenominatorAndRightDenominatorLeftQuantity,
                        RightDenominatorRightQuantity,
                        >,
                RightDenominatorUnit,
                >,
        TargetNumeratorUnit : UndefinedMultipliedUnit<
                UndefinedQuantityType.Multiplying<
                        LeftNumeratorLeftAndRightNumeratorLeftQuantity,
                        LeftNumeratorRightQuantity,
                        >,
                LeftNumeratorUnit,
                UndefinedQuantityType.Multiplying<
                        LeftNumeratorLeftAndRightNumeratorLeftQuantity,
                        RightNumeratorRightQuantity,
                        >,
                RightNumeratorUnit,
                >,
        TargetDenominatorLeftUnit : UndefinedMultipliedUnit<
                LeftDenominatorAndRightDenominatorLeftQuantity,
                LeftDenominatorUnit,
                LeftDenominatorAndRightDenominatorLeftQuantity,
                LeftDenominatorUnit,
                >,
        TargetDenominatorUnit : UndefinedMultipliedUnit<
                UndefinedQuantityType.Multiplying<
                        LeftDenominatorAndRightDenominatorLeftQuantity,
                        LeftDenominatorAndRightDenominatorLeftQuantity,
                        >,
                TargetDenominatorLeftUnit,
                RightDenominatorRightQuantity,
                RightDenominatorRightUnit,
                >,
        TargetUnit : UndefinedDividedUnit<
                UndefinedQuantityType.Multiplying<
                        UndefinedQuantityType.Multiplying<
                                LeftNumeratorLeftAndRightNumeratorLeftQuantity,
                                LeftNumeratorRightQuantity,
                                >,
                        UndefinedQuantityType.Multiplying<
                                LeftNumeratorLeftAndRightNumeratorLeftQuantity,
                                RightNumeratorRightQuantity,
                                >,
                        >,
                TargetNumeratorUnit,
                UndefinedQuantityType.Multiplying<
                        UndefinedQuantityType.Multiplying<
                                LeftDenominatorAndRightDenominatorLeftQuantity,
                                LeftDenominatorAndRightDenominatorLeftQuantity,
                                >,
                        RightDenominatorRightQuantity,
                        >,
                TargetDenominatorUnit,
                >,
        TargetValue : UndefinedScientificValue<
                UndefinedQuantityType.Dividing<
                        UndefinedQuantityType.Multiplying<
                                UndefinedQuantityType.Multiplying<
                                        LeftNumeratorLeftAndRightNumeratorLeftQuantity,
                                        LeftNumeratorRightQuantity,
                                        >,
                                UndefinedQuantityType.Multiplying<
                                        LeftNumeratorLeftAndRightNumeratorLeftQuantity,
                                        RightNumeratorRightQuantity,
                                        >,
                                >,
                        UndefinedQuantityType.Multiplying<
                                UndefinedQuantityType.Multiplying<
                                        LeftDenominatorAndRightDenominatorLeftQuantity,
                                        LeftDenominatorAndRightDenominatorLeftQuantity,
                                        >,
                                RightDenominatorRightQuantity,
                                >,
                        >,
                TargetUnit,
                >,
        > UndefinedScientificValue<
        UndefinedQuantityType.Dividing<
                UndefinedQuantityType.Multiplying<
                        LeftNumeratorLeftAndRightNumeratorLeftQuantity,
                        LeftNumeratorRightQuantity,
                        >,
                LeftDenominatorAndRightDenominatorLeftQuantity,
                >,
        LeftUnit,
        >.multipliedBy(
    right: UndefinedScientificValue<
            UndefinedQuantityType.Dividing<
                    UndefinedQuantityType.Multiplying<
                            LeftNumeratorLeftAndRightNumeratorLeftQuantity,
                            RightNumeratorRightQuantity,
                            >,
                    UndefinedQuantityType.Multiplying<
                            LeftDenominatorAndRightDenominatorLeftQuantity,
                            RightDenominatorRightQuantity,
                            >,
                    >,
            RightUnit,
            >,
    leftNumeratorUnitXRightNumeratorUnit: LeftNumeratorUnit.(RightNumeratorUnit) -> TargetNumeratorUnit,
    leftDenominatorUnitXLeftDenominatorUnit: LeftDenominatorUnit.(LeftDenominatorUnit) -> TargetDenominatorLeftUnit,
    targetDenominatorLeftUnitXRightDenominatorRightUnit: TargetDenominatorLeftUnit.(RightDenominatorRightUnit) -> TargetDenominatorUnit,
    targetNumeratorUnitPerTargetDenominatorUnit: TargetNumeratorUnit.(TargetDenominatorUnit) -> TargetUnit,
    factory: (Decimal, TargetUnit) -> TargetValue,
) = internalDividingWithMultiplyingNumeratorMultipliedByDividingUnitWithMultiplyingNumeratorWithNumeratorLeftAsLeftAndMultiplyingDenominatorWithDenominatorAsLeft(
    right = right,
    leftNumeratorUnitXRightNumeratorUnit = leftNumeratorUnitXRightNumeratorUnit,
    leftDenominatorUnitXLeftDenominatorUnit = leftDenominatorUnitXLeftDenominatorUnit,
    targetDenominatorLeftUnitXRightDenominatorRightUnit = targetDenominatorLeftUnitXRightDenominatorRightUnit,
    targetNumeratorUnitPerTargetDenominatorUnit = targetNumeratorUnitPerTargetDenominatorUnit,
    factory = factory,
)

fun <
        TargetQuantity : PhysicalQuantity,
        TargetUnit : ScientificUnit<TargetQuantity>,
        Value : ScientificValue<TargetQuantity, TargetUnit>,
        LeftQuantity : PhysicalQuantity,
        LeftUnit : ScientificUnit<LeftQuantity>,
        RightQuantity : PhysicalQuantity,
        RightUnit : ScientificUnit<RightQuantity>,
        > TargetUnit.byMultiplying(
    left: ScientificValue<LeftQuantity, LeftUnit>,
    right: ScientificValue<RightQuantity, RightUnit>,
    factory: (Decimal, TargetUnit) -> Value,
): Decimal = TODO()


/* GENERATED_FIR_TAGS: classDeclaration, data, funWithExtensionReceiver, functionDeclaration, functionalType,
interfaceDeclaration, nestedClass, nullableType, primaryConstructor, propertyDeclaration, sealed, starProjection,
stringLiteral, thisExpression, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeConstraint,
typeParameter, typeWithExtension */
