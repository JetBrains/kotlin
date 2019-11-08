__attribute__((swift_name("KotlinBase")))
@interface ValuesBase : NSObject
- (instancetype)init __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
+ (void)initialize __attribute__((objc_requires_super));
@end;

@interface ValuesBase (ValuesBaseCopying) <NSCopying>
@end;

__attribute__((swift_name("KotlinMutableSet")))
@interface ValuesMutableSet<ObjectType> : NSMutableSet<ObjectType>
@end;

__attribute__((swift_name("KotlinMutableDictionary")))
@interface ValuesMutableDictionary<KeyType, ObjectType> : NSMutableDictionary<KeyType, ObjectType>
@end;

@interface NSError (NSErrorValuesKotlinException)
@property (readonly) id _Nullable kotlinException;
@end;

__attribute__((swift_name("KotlinNumber")))
@interface ValuesNumber : NSNumber
- (instancetype)initWithChar:(char)value __attribute__((unavailable));
- (instancetype)initWithUnsignedChar:(unsigned char)value __attribute__((unavailable));
- (instancetype)initWithShort:(short)value __attribute__((unavailable));
- (instancetype)initWithUnsignedShort:(unsigned short)value __attribute__((unavailable));
- (instancetype)initWithInt:(int)value __attribute__((unavailable));
- (instancetype)initWithUnsignedInt:(unsigned int)value __attribute__((unavailable));
- (instancetype)initWithLong:(long)value __attribute__((unavailable));
- (instancetype)initWithUnsignedLong:(unsigned long)value __attribute__((unavailable));
- (instancetype)initWithLongLong:(long long)value __attribute__((unavailable));
- (instancetype)initWithUnsignedLongLong:(unsigned long long)value __attribute__((unavailable));
- (instancetype)initWithFloat:(float)value __attribute__((unavailable));
- (instancetype)initWithDouble:(double)value __attribute__((unavailable));
- (instancetype)initWithBool:(BOOL)value __attribute__((unavailable));
- (instancetype)initWithInteger:(NSInteger)value __attribute__((unavailable));
- (instancetype)initWithUnsignedInteger:(NSUInteger)value __attribute__((unavailable));
+ (instancetype)numberWithChar:(char)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedChar:(unsigned char)value __attribute__((unavailable));
+ (instancetype)numberWithShort:(short)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedShort:(unsigned short)value __attribute__((unavailable));
+ (instancetype)numberWithInt:(int)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedInt:(unsigned int)value __attribute__((unavailable));
+ (instancetype)numberWithLong:(long)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedLong:(unsigned long)value __attribute__((unavailable));
+ (instancetype)numberWithLongLong:(long long)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedLongLong:(unsigned long long)value __attribute__((unavailable));
+ (instancetype)numberWithFloat:(float)value __attribute__((unavailable));
+ (instancetype)numberWithDouble:(double)value __attribute__((unavailable));
+ (instancetype)numberWithBool:(BOOL)value __attribute__((unavailable));
+ (instancetype)numberWithInteger:(NSInteger)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedInteger:(NSUInteger)value __attribute__((unavailable));
@end;

__attribute__((swift_name("KotlinByte")))
@interface ValuesByte : ValuesNumber
- (instancetype)initWithChar:(char)value;
+ (instancetype)numberWithChar:(char)value;
@end;

__attribute__((swift_name("KotlinUByte")))
@interface ValuesUByte : ValuesNumber
- (instancetype)initWithUnsignedChar:(unsigned char)value;
+ (instancetype)numberWithUnsignedChar:(unsigned char)value;
@end;

__attribute__((swift_name("KotlinShort")))
@interface ValuesShort : ValuesNumber
- (instancetype)initWithShort:(short)value;
+ (instancetype)numberWithShort:(short)value;
@end;

__attribute__((swift_name("KotlinUShort")))
@interface ValuesUShort : ValuesNumber
- (instancetype)initWithUnsignedShort:(unsigned short)value;
+ (instancetype)numberWithUnsignedShort:(unsigned short)value;
@end;

__attribute__((swift_name("KotlinInt")))
@interface ValuesInt : ValuesNumber
- (instancetype)initWithInt:(int)value;
+ (instancetype)numberWithInt:(int)value;
@end;

__attribute__((swift_name("KotlinUInt")))
@interface ValuesUInt : ValuesNumber
- (instancetype)initWithUnsignedInt:(unsigned int)value;
+ (instancetype)numberWithUnsignedInt:(unsigned int)value;
@end;

__attribute__((swift_name("KotlinLong")))
@interface ValuesLong : ValuesNumber
- (instancetype)initWithLongLong:(long long)value;
+ (instancetype)numberWithLongLong:(long long)value;
@end;

__attribute__((swift_name("KotlinULong")))
@interface ValuesULong : ValuesNumber
- (instancetype)initWithUnsignedLongLong:(unsigned long long)value;
+ (instancetype)numberWithUnsignedLongLong:(unsigned long long)value;
@end;

__attribute__((swift_name("KotlinFloat")))
@interface ValuesFloat : ValuesNumber
- (instancetype)initWithFloat:(float)value;
+ (instancetype)numberWithFloat:(float)value;
@end;

__attribute__((swift_name("KotlinDouble")))
@interface ValuesDouble : ValuesNumber
- (instancetype)initWithDouble:(double)value;
+ (instancetype)numberWithDouble:(double)value;
@end;

__attribute__((swift_name("KotlinBoolean")))
@interface ValuesBoolean : ValuesNumber
- (instancetype)initWithBool:(BOOL)value;
+ (instancetype)numberWithBool:(BOOL)value;
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DelegateClass")))
@interface ValuesDelegateClass : ValuesBase <ValuesKotlinReadWriteProperty>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (ValuesKotlinArray *)getValueThisRef:(ValuesKotlinNothing * _Nullable)thisRef property:(id<ValuesKotlinKProperty>)property __attribute__((swift_name("getValue(thisRef:property:)")));
- (void)setValueThisRef:(ValuesKotlinNothing * _Nullable)thisRef property:(id<ValuesKotlinKProperty>)property value:(ValuesKotlinArray *)value __attribute__((swift_name("setValue(thisRef:property:value:)")));
@end;

__attribute__((swift_name("I")))
@protocol ValuesI
@required
- (NSString *)iFun __attribute__((swift_name("iFun()")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DefaultInterfaceExt")))
@interface ValuesDefaultInterfaceExt : ValuesBase <ValuesI>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((swift_name("OpenClassI")))
@interface ValuesOpenClassI : ValuesBase <ValuesI>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (NSString *)iFun __attribute__((swift_name("iFun()")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("FinalClassExtOpen")))
@interface ValuesFinalClassExtOpen : ValuesOpenClassI
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (NSString *)iFun __attribute__((swift_name("iFun()")));
@end;

__attribute__((swift_name("MultiExtClass")))
@interface ValuesMultiExtClass : ValuesOpenClassI
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (id)piFun __attribute__((swift_name("piFun()")));
- (NSString *)iFun __attribute__((swift_name("iFun()")));
@end;

__attribute__((swift_name("ConstrClass")))
@interface ValuesConstrClass : ValuesOpenClassI
- (instancetype)initWithI:(int32_t)i s:(NSString *)s a:(id)a __attribute__((swift_name("init(i:s:a:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
@property (readonly) int32_t i __attribute__((swift_name("i")));
@property (readonly) NSString *s __attribute__((swift_name("s")));
@property (readonly) id a __attribute__((swift_name("a")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ExtConstrClass")))
@interface ValuesExtConstrClass : ValuesConstrClass
- (instancetype)initWithI:(int32_t)i __attribute__((swift_name("init(i:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithI:(int32_t)i s:(NSString *)s a:(id)a __attribute__((swift_name("init(i:s:a:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (NSString *)iFun __attribute__((swift_name("iFun()")));
@property (readonly) int32_t i __attribute__((swift_name("i")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Enumeration")))
@interface ValuesEnumeration : ValuesKotlinEnum
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) ValuesEnumeration *answer __attribute__((swift_name("answer")));
@property (class, readonly) ValuesEnumeration *year __attribute__((swift_name("year")));
@property (class, readonly) ValuesEnumeration *temperature __attribute__((swift_name("temperature")));
- (int32_t)compareToOther:(ValuesEnumeration *)other __attribute__((swift_name("compareTo(other:)")));
@property (readonly) int32_t enumValue __attribute__((swift_name("enumValue")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TripleVals")))
@interface ValuesTripleVals : ValuesBase
- (instancetype)initWithFirst:(id _Nullable)first second:(id _Nullable)second third:(id _Nullable)third __attribute__((swift_name("init(first:second:third:)"))) __attribute__((objc_designated_initializer));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
- (id _Nullable)component1 __attribute__((swift_name("component1()")));
- (id _Nullable)component2 __attribute__((swift_name("component2()")));
- (id _Nullable)component3 __attribute__((swift_name("component3()")));
- (ValuesTripleVals *)doCopyFirst:(id _Nullable)first second:(id _Nullable)second third:(id _Nullable)third __attribute__((swift_name("doCopy(first:second:third:)")));
@property (readonly) id _Nullable first __attribute__((swift_name("first")));
@property (readonly) id _Nullable second __attribute__((swift_name("second")));
@property (readonly) id _Nullable third __attribute__((swift_name("third")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TripleVars")))
@interface ValuesTripleVars : ValuesBase
- (instancetype)initWithFirst:(id _Nullable)first second:(id _Nullable)second third:(id _Nullable)third __attribute__((swift_name("init(first:second:third:)"))) __attribute__((objc_designated_initializer));
- (NSString *)description __attribute__((swift_name("description()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (id _Nullable)component1 __attribute__((swift_name("component1()")));
- (id _Nullable)component2 __attribute__((swift_name("component2()")));
- (id _Nullable)component3 __attribute__((swift_name("component3()")));
- (ValuesTripleVars *)doCopyFirst:(id _Nullable)first second:(id _Nullable)second third:(id _Nullable)third __attribute__((swift_name("doCopy(first:second:third:)")));
@property id _Nullable first __attribute__((swift_name("first")));
@property id _Nullable second __attribute__((swift_name("second")));
@property id _Nullable third __attribute__((swift_name("third")));
@end;

__attribute__((swift_name("WithCompanionAndObject")))
@interface ValuesWithCompanionAndObject : ValuesBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WithCompanionAndObject.Companion")))
@interface ValuesWithCompanionAndObjectCompanion : ValuesBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (readonly) NSString *str __attribute__((swift_name("str")));
@property id<ValuesI> _Nullable named __attribute__((swift_name("named")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WithCompanionAndObject.Named")))
@interface ValuesWithCompanionAndObjectNamed : ValuesOpenClassI
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
+ (instancetype)named __attribute__((swift_name("init()")));
- (NSString *)iFun __attribute__((swift_name("iFun()")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MyException")))
@interface ValuesMyException : ValuesKotlinException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(ValuesKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithCause:(ValuesKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@end;

__attribute__((swift_name("BridgeBase")))
@interface ValuesBridgeBase : ValuesBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (id _Nullable)foo1AndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("foo1()")));
- (BOOL)foo2AndReturnResult:(int32_t * _Nullable)result error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("foo2(result:)")));
- (BOOL)foo3AndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("foo3()")));
- (BOOL)foo4AndReturnResult:(ValuesKotlinNothing * _Nullable * _Nullable)result error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("foo4(result:)")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Bridge")))
@interface ValuesBridge : ValuesBridgeBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (ValuesKotlinNothing * _Nullable)foo1AndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("foo1()")));
- (BOOL)foo2AndReturnResult:(int32_t * _Nullable)result error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("foo2(result:)")));
- (BOOL)foo3AndReturnError:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("foo3()")));
- (BOOL)foo4AndReturnResult:(ValuesKotlinNothing * _Nullable * _Nullable)result error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("foo4(result:)")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Deeply")))
@interface ValuesDeeply : ValuesBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Deeply.Nested")))
@interface ValuesDeeplyNested : ValuesBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Deeply.NestedType")))
@interface ValuesDeeplyNestedType : ValuesBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly) int32_t thirtyTwo __attribute__((swift_name("thirtyTwo")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WithGenericDeeply")))
@interface ValuesWithGenericDeeply : ValuesBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WithGenericDeeply.Nested")))
@interface ValuesWithGenericDeeplyNested : ValuesBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WithGenericDeeply.NestedType")))
@interface ValuesWithGenericDeeplyNestedType : ValuesBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly) int32_t thirtyThree __attribute__((swift_name("thirtyThree")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TypeOuter")))
@interface ValuesTypeOuter : ValuesBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TypeOuter.Type_")))
@interface ValuesTypeOuterType_ : ValuesBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly) int32_t thirtyFour __attribute__((swift_name("thirtyFour")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CKeywords")))
@interface ValuesCKeywords : ValuesBase
- (instancetype)initWithFloat:(float)float_ enum:(int32_t)enum_ goto:(BOOL)goto_ __attribute__((swift_name("init(float:enum:goto:)"))) __attribute__((objc_designated_initializer));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
- (float)component1 __attribute__((swift_name("component1()")));
- (int32_t)component2 __attribute__((swift_name("component2()")));
- (BOOL)component3 __attribute__((swift_name("component3()")));
- (ValuesCKeywords *)doCopyFloat:(float)float_ enum:(int32_t)enum_ goto:(BOOL)goto_ __attribute__((swift_name("doCopy(float:enum:goto:)")));
@property (readonly, getter=float) float float_ __attribute__((swift_name("float_")));
@property (readonly, getter=enum) int32_t enum_ __attribute__((swift_name("enum_")));
@property (getter=goto, setter=setGoto:) BOOL goto_ __attribute__((swift_name("goto_")));
@end;

__attribute__((swift_name("Base1")))
@protocol ValuesBase1
@required
- (ValuesInt * _Nullable)sameValue:(ValuesInt * _Nullable)value __attribute__((swift_name("same(value:)")));
@end;

__attribute__((swift_name("ExtendedBase1")))
@protocol ValuesExtendedBase1 <ValuesBase1>
@required
@end;

__attribute__((swift_name("Base2")))
@protocol ValuesBase2
@required
- (ValuesInt * _Nullable)sameValue:(ValuesInt * _Nullable)value __attribute__((swift_name("same(value:)")));
@end;

__attribute__((swift_name("Base23")))
@interface ValuesBase23 : ValuesBase <ValuesBase2>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (ValuesInt *)sameValue:(ValuesInt * _Nullable)value __attribute__((swift_name("same(value:)")));
@end;

__attribute__((swift_name("Transform")))
@protocol ValuesTransform
@required
- (id _Nullable)mapValue:(id _Nullable)value __attribute__((swift_name("map(value:)")));
@end;

__attribute__((swift_name("TransformWithDefault")))
@protocol ValuesTransformWithDefault <ValuesTransform>
@required
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TransformInheritingDefault")))
@interface ValuesTransformInheritingDefault : ValuesBase <ValuesTransformWithDefault>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((swift_name("TransformIntString")))
@protocol ValuesTransformIntString
@required
- (NSString *)mapIntValue:(int32_t)intValue __attribute__((swift_name("map(intValue:)")));
@end;

__attribute__((swift_name("TransformIntToString")))
@interface ValuesTransformIntToString : ValuesBase <ValuesTransform, ValuesTransformIntString>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (NSString *)mapValue:(ValuesInt *)intValue __attribute__((swift_name("map(value:)")));
- (NSString *)mapIntValue:(int32_t)intValue __attribute__((swift_name("map(intValue:)")));
@end;

__attribute__((swift_name("TransformIntToDecimalString")))
@interface ValuesTransformIntToDecimalString : ValuesTransformIntToString
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (NSString *)mapValue:(ValuesInt *)intValue __attribute__((swift_name("map(value:)")));
- (NSString *)mapIntValue:(int32_t)intValue __attribute__((swift_name("map(intValue:)")));
@end;

__attribute__((swift_name("TransformIntToLong")))
@interface ValuesTransformIntToLong : ValuesBase <ValuesTransform>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (ValuesLong *)mapValue:(ValuesInt *)value __attribute__((swift_name("map(value:)")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GH2931")))
@interface ValuesGH2931 : ValuesBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GH2931.Data")))
@interface ValuesGH2931Data : ValuesBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GH2931.Holder")))
@interface ValuesGH2931Holder : ValuesBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly) ValuesGH2931Data *data __attribute__((swift_name("data")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GH2945")))
@interface ValuesGH2945 : ValuesBase
- (instancetype)initWithErrno:(int32_t)errno __attribute__((swift_name("init(errno:)"))) __attribute__((objc_designated_initializer));
- (int32_t)testErrnoInSelectorP:(int32_t)p errno:(int32_t)errno __attribute__((swift_name("testErrnoInSelector(p:errno:)")));
@property int32_t errno __attribute__((swift_name("errno")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GH2830")))
@interface ValuesGH2830 : ValuesBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (id)getI __attribute__((swift_name("getI()")));
@end;

__attribute__((swift_name("GH2830I")))
@protocol ValuesGH2830I
@required
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GH2959")))
@interface ValuesGH2959 : ValuesBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (NSArray<id<ValuesGH2959I>> *)getIId:(int32_t)id __attribute__((swift_name("getI(id:)")));
@end;

__attribute__((swift_name("GH2959I")))
@protocol ValuesGH2959I
@required
@property (readonly) int32_t id __attribute__((swift_name("id")));
@end;

__attribute__((swift_name("IntBlocks")))
@protocol ValuesIntBlocks
@required
- (id _Nullable)getPlusOneBlock __attribute__((swift_name("getPlusOneBlock()")));
- (int32_t)callBlockArgument:(int32_t)argument block:(id _Nullable)block __attribute__((swift_name("callBlock(argument:block:)")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("IntBlocksImpl")))
@interface ValuesIntBlocksImpl : ValuesBase <ValuesIntBlocks>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)intBlocksImpl __attribute__((swift_name("init()")));
- (ValuesInt *(^)(ValuesInt *))getPlusOneBlock __attribute__((swift_name("getPlusOneBlock()")));
- (int32_t)callBlockArgument:(int32_t)argument block:(ValuesInt *(^)(ValuesInt *))block __attribute__((swift_name("callBlock(argument:block:)")));
@end;

__attribute__((swift_name("UnitBlockCoercion")))
@protocol ValuesUnitBlockCoercion
@required
- (id)coerceBlock:(void (^)(void))block __attribute__((swift_name("coerce(block:)")));
- (void (^)(void))uncoerceBlock:(id)block __attribute__((swift_name("uncoerce(block:)")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UnitBlockCoercionImpl")))
@interface ValuesUnitBlockCoercionImpl : ValuesBase <ValuesUnitBlockCoercion>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)unitBlockCoercionImpl __attribute__((swift_name("init()")));
- (ValuesKotlinUnit *(^)(void))coerceBlock:(void (^)(void))block __attribute__((swift_name("coerce(block:)")));
- (void (^)(void))uncoerceBlock:(ValuesKotlinUnit *(^)(void))block __attribute__((swift_name("uncoerce(block:)")));
@end;

__attribute__((swift_name("MyAbstractList")))
@interface ValuesMyAbstractList : NSObject
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestKClass")))
@interface ValuesTestKClass : ValuesBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (id<ValuesKotlinKClass> _Nullable)getKotlinClassClazz:(Class)clazz __attribute__((swift_name("getKotlinClass(clazz:)")));
- (id<ValuesKotlinKClass> _Nullable)getKotlinClassProtocol:(Protocol *)protocol __attribute__((swift_name("getKotlinClass(protocol:)")));
- (BOOL)isTestKClassKClass:(id<ValuesKotlinKClass>)kClass __attribute__((swift_name("isTestKClass(kClass:)")));
- (BOOL)isIKClass:(id<ValuesKotlinKClass>)kClass __attribute__((swift_name("isI(kClass:)")));
@end;

__attribute__((swift_name("TestKClassI")))
@protocol ValuesTestKClassI
@required
@end;

__attribute__((swift_name("ForwardI2")))
@protocol ValuesForwardI2 <ValuesForwardI1>
@required
@end;

__attribute__((swift_name("ForwardI1")))
@protocol ValuesForwardI1
@required
- (id<ValuesForwardI2>)getForwardI2 __attribute__((swift_name("getForwardI2()")));
@end;

__attribute__((swift_name("ForwardC2")))
@interface ValuesForwardC2 : ValuesForwardC1
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((swift_name("ForwardC1")))
@interface ValuesForwardC1 : ValuesBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (ValuesForwardC2 *)getForwardC2 __attribute__((swift_name("getForwardC2()")));
@end;

__attribute__((swift_name("TestSR10177Workaround")))
@protocol ValuesTestSR10177Workaround
@required
@end;

__attribute__((swift_name("TestClashes1")))
@protocol ValuesTestClashes1
@required
@property (readonly) int32_t clashingProperty __attribute__((swift_name("clashingProperty")));
@end;

__attribute__((swift_name("TestClashes2")))
@protocol ValuesTestClashes2
@required
@property (readonly) id clashingProperty __attribute__((swift_name("clashingProperty")));
@property (readonly) id clashingProperty_ __attribute__((swift_name("clashingProperty_")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestClashesImpl")))
@interface ValuesTestClashesImpl : ValuesBase <ValuesTestClashes1, ValuesTestClashes2>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly) int32_t clashingProperty __attribute__((swift_name("clashingProperty")));
@property (readonly) ValuesInt *clashingProperty_ __attribute__((swift_name("clashingProperty_")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestInvalidIdentifiers")))
@interface ValuesTestInvalidIdentifiers : ValuesBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (int32_t)a_d_d_1:(int32_t)_1 _2:(int32_t)_2 _3:(int32_t)_3 __attribute__((swift_name("a_d_d(_1:_2:_3:)")));
@property NSString *_status __attribute__((swift_name("_status")));
@property (readonly) unichar __ __attribute__((swift_name("__")));
@property (readonly) unichar __ __attribute__((swift_name("__")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestInvalidIdentifiers._Foo")))
@interface ValuesTestInvalidIdentifiers_Foo : ValuesBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestInvalidIdentifiers.Bar_")))
@interface ValuesTestInvalidIdentifiersBar_ : ValuesBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestInvalidIdentifiers.E")))
@interface ValuesTestInvalidIdentifiersE : ValuesKotlinEnum
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) ValuesTestInvalidIdentifiersE *_4_ __attribute__((swift_name("_4_")));
@property (class, readonly) ValuesTestInvalidIdentifiersE *_5_ __attribute__((swift_name("_5_")));
@property (class, readonly) ValuesTestInvalidIdentifiersE *__ __attribute__((swift_name("__")));
@property (class, readonly) ValuesTestInvalidIdentifiersE *__ __attribute__((swift_name("__")));
- (int32_t)compareToOther:(ValuesTestInvalidIdentifiersE *)other __attribute__((swift_name("compareTo(other:)")));
@property (readonly) int32_t value __attribute__((swift_name("value")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestInvalidIdentifiers.Companion_")))
@interface ValuesTestInvalidIdentifiersCompanion_ : ValuesBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion_ __attribute__((swift_name("init()")));
@property (readonly) int32_t _42 __attribute__((swift_name("_42")));
@end;

__attribute__((swift_name("TestDeprecation")))
@interface ValuesTestDeprecation : ValuesBase
- (instancetype)initWithError:(int16_t)error __attribute__((swift_name("init(error:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable("error")));
- (instancetype)initWithWarning:(int32_t)warning __attribute__((swift_name("init(warning:)"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("warning")));
- (instancetype)initWithNormal:(int64_t)normal __attribute__((swift_name("init(normal:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (int32_t)callEffectivelyHiddenObj:(id)obj __attribute__((swift_name("callEffectivelyHidden(obj:)")));
- (id)getHidden __attribute__((swift_name("getHidden()")));
- (ValuesTestDeprecationError *)getError __attribute__((swift_name("getError()")));
- (void)error __attribute__((swift_name("error()"))) __attribute__((unavailable("error")));
- (void)openError __attribute__((swift_name("openError()"))) __attribute__((unavailable("error")));
- (ValuesTestDeprecationWarning *)getWarning __attribute__((swift_name("getWarning()")));
- (void)warning __attribute__((swift_name("warning()"))) __attribute__((deprecated("warning")));
- (void)openWarning __attribute__((swift_name("openWarning()"))) __attribute__((deprecated("warning")));
- (void)normal __attribute__((swift_name("normal()")));
- (int32_t)openNormal __attribute__((swift_name("openNormal()")));
- (void)testHiddenNested:(id)hiddenNested __attribute__((swift_name("test(hiddenNested:)")));
- (void)testHiddenNestedNested:(id)hiddenNestedNested __attribute__((swift_name("test(hiddenNestedNested:)")));
- (void)testHiddenNestedInner:(id)hiddenNestedInner __attribute__((swift_name("test(hiddenNestedInner:)")));
- (void)testHiddenInner:(id)hiddenInner __attribute__((swift_name("test(hiddenInner:)")));
- (void)testHiddenInnerInner:(id)hiddenInnerInner __attribute__((swift_name("test(hiddenInnerInner:)")));
- (void)testTopLevelHidden:(id)topLevelHidden __attribute__((swift_name("test(topLevelHidden:)")));
- (void)testTopLevelHiddenNested:(id)topLevelHiddenNested __attribute__((swift_name("test(topLevelHiddenNested:)")));
- (void)testTopLevelHiddenNestedNested:(id)topLevelHiddenNestedNested __attribute__((swift_name("test(topLevelHiddenNestedNested:)")));
- (void)testTopLevelHiddenNestedInner:(id)topLevelHiddenNestedInner __attribute__((swift_name("test(topLevelHiddenNestedInner:)")));
- (void)testTopLevelHiddenInner:(id)topLevelHiddenInner __attribute__((swift_name("test(topLevelHiddenInner:)")));
- (void)testTopLevelHiddenInnerInner:(id)topLevelHiddenInnerInner __attribute__((swift_name("test(topLevelHiddenInnerInner:)")));
- (void)testExtendingHiddenNested:(id)extendingHiddenNested __attribute__((swift_name("test(extendingHiddenNested:)")));
- (void)testExtendingNestedInHidden:(id)extendingNestedInHidden __attribute__((swift_name("test(extendingNestedInHidden:)")));
@property (readonly) id _Nullable errorVal __attribute__((swift_name("errorVal"))) __attribute__((unavailable("error")));
@property id _Nullable errorVar __attribute__((swift_name("errorVar"))) __attribute__((unavailable("error")));
@property (readonly) id _Nullable openErrorVal __attribute__((swift_name("openErrorVal"))) __attribute__((unavailable("error")));
@property id _Nullable openErrorVar __attribute__((swift_name("openErrorVar"))) __attribute__((unavailable("error")));
@property (readonly) id _Nullable warningVal __attribute__((swift_name("warningVal"))) __attribute__((deprecated("warning")));
@property id _Nullable warningVar __attribute__((swift_name("warningVar"))) __attribute__((deprecated("warning")));
@property (readonly) id _Nullable openWarningVal __attribute__((swift_name("openWarningVal"))) __attribute__((deprecated("warning")));
@property id _Nullable openWarningVar __attribute__((swift_name("openWarningVar"))) __attribute__((deprecated("warning")));
@property (readonly) id _Nullable normalVal __attribute__((swift_name("normalVal")));
@property id _Nullable normalVar __attribute__((swift_name("normalVar")));
@property (readonly) id _Nullable openNormalVal __attribute__((swift_name("openNormalVal")));
@property id _Nullable openNormalVar __attribute__((swift_name("openNormalVar")));
@end;

__attribute__((swift_name("TestDeprecation.OpenHidden")))
@interface ValuesTestDeprecationOpenHidden : NSObject
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.ExtendingHidden")))
@interface ValuesTestDeprecationExtendingHidden : NSObject
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.ExtendingHiddenNested")))
@interface ValuesTestDeprecationExtendingHiddenNested : NSObject
@end;

__attribute__((swift_name("TestDeprecationHiddenInterface")))
@protocol ValuesTestDeprecationHiddenInterface
@required
@end;

__attribute__((swift_name("TestDeprecation.ImplementingHidden")))
@interface ValuesTestDeprecationImplementingHidden : ValuesBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (int32_t)effectivelyHidden __attribute__((swift_name("effectivelyHidden()")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.Hidden")))
@interface ValuesTestDeprecationHidden : NSObject
@end;

__attribute__((swift_name("TestDeprecation.HiddenNested")))
@interface ValuesTestDeprecationHiddenNested : NSObject
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.HiddenNestedNested")))
@interface ValuesTestDeprecationHiddenNestedNested : NSObject
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.HiddenNestedInner")))
@interface ValuesTestDeprecationHiddenNestedInner : NSObject
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.HiddenInner")))
@interface ValuesTestDeprecationHiddenInner : NSObject
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.HiddenInnerInner")))
@interface ValuesTestDeprecationHiddenInnerInner : NSObject
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.ExtendingNestedInHidden")))
@interface ValuesTestDeprecationExtendingNestedInHidden : NSObject
@end;

__attribute__((swift_name("TestDeprecation.OpenError")))
@interface ValuesTestDeprecationOpenError : ValuesTestDeprecation
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable("error")));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithError:(int16_t)error __attribute__((swift_name("init(error:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithWarning:(int32_t)warning __attribute__((swift_name("init(warning:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithNormal:(int64_t)normal __attribute__((swift_name("init(normal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.ExtendingError")))
@interface ValuesTestDeprecationExtendingError : ValuesTestDeprecationOpenError
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((swift_name("TestDeprecationErrorInterface")))
@protocol ValuesTestDeprecationErrorInterface
@required
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.ImplementingError")))
@interface ValuesTestDeprecationImplementingError : ValuesBase <ValuesTestDeprecationErrorInterface>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.Error")))
@interface ValuesTestDeprecationError : ValuesTestDeprecation
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable("error")));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithError:(int16_t)error __attribute__((swift_name("init(error:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithWarning:(int32_t)warning __attribute__((swift_name("init(warning:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithNormal:(int64_t)normal __attribute__((swift_name("init(normal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@end;

__attribute__((swift_name("TestDeprecation.OpenWarning")))
@interface ValuesTestDeprecationOpenWarning : ValuesTestDeprecation
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("warning")));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithError:(int16_t)error __attribute__((swift_name("init(error:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithWarning:(int32_t)warning __attribute__((swift_name("init(warning:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithNormal:(int64_t)normal __attribute__((swift_name("init(normal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.ExtendingWarning")))
@interface ValuesTestDeprecationExtendingWarning : ValuesTestDeprecationOpenWarning
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((swift_name("TestDeprecationWarningInterface")))
@protocol ValuesTestDeprecationWarningInterface
@required
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.ImplementingWarning")))
@interface ValuesTestDeprecationImplementingWarning : ValuesBase <ValuesTestDeprecationWarningInterface>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.Warning")))
@interface ValuesTestDeprecationWarning : ValuesTestDeprecation
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("warning")));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithError:(int16_t)error __attribute__((swift_name("init(error:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithWarning:(int32_t)warning __attribute__((swift_name("init(warning:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithNormal:(int64_t)normal __attribute__((swift_name("init(normal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.HiddenOverride")))
@interface ValuesTestDeprecationHiddenOverride : ValuesTestDeprecation
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithError:(int16_t)error __attribute__((swift_name("init(error:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithWarning:(int32_t)warning __attribute__((swift_name("init(warning:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithNormal:(int64_t)normal __attribute__((swift_name("init(normal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (void)openError __attribute__((swift_name("openError()"))) __attribute__((unavailable("hidden")));
- (void)openWarning __attribute__((swift_name("openWarning()"))) __attribute__((unavailable("hidden")));
- (int32_t)openNormal __attribute__((swift_name("openNormal()"))) __attribute__((unavailable("hidden")));
@property (readonly) id _Nullable openErrorVal __attribute__((swift_name("openErrorVal"))) __attribute__((unavailable("hidden")));
@property id _Nullable openErrorVar __attribute__((swift_name("openErrorVar"))) __attribute__((unavailable("hidden")));
@property (readonly) id _Nullable openWarningVal __attribute__((swift_name("openWarningVal"))) __attribute__((unavailable("hidden")));
@property id _Nullable openWarningVar __attribute__((swift_name("openWarningVar"))) __attribute__((unavailable("hidden")));
@property (readonly) id _Nullable openNormalVal __attribute__((swift_name("openNormalVal"))) __attribute__((unavailable("hidden")));
@property id _Nullable openNormalVar __attribute__((swift_name("openNormalVar"))) __attribute__((unavailable("hidden")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.ErrorOverride")))
@interface ValuesTestDeprecationErrorOverride : ValuesTestDeprecation
- (instancetype)initWithHidden:(int8_t)hidden __attribute__((swift_name("init(hidden:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable("error")));
- (instancetype)initWithError:(int16_t)error __attribute__((swift_name("init(error:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable("error")));
- (instancetype)initWithWarning:(int32_t)warning __attribute__((swift_name("init(warning:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable("error")));
- (instancetype)initWithNormal:(int64_t)normal __attribute__((swift_name("init(normal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable("error")));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)openHidden __attribute__((swift_name("openHidden()"))) __attribute__((unavailable("error")));
- (void)openError __attribute__((swift_name("openError()"))) __attribute__((unavailable("error")));
- (void)openWarning __attribute__((swift_name("openWarning()"))) __attribute__((unavailable("error")));
- (int32_t)openNormal __attribute__((swift_name("openNormal()"))) __attribute__((unavailable("error")));
@property (readonly) id _Nullable openHiddenVal __attribute__((swift_name("openHiddenVal"))) __attribute__((unavailable("error")));
@property id _Nullable openHiddenVar __attribute__((swift_name("openHiddenVar"))) __attribute__((unavailable("error")));
@property (readonly) id _Nullable openErrorVal __attribute__((swift_name("openErrorVal"))) __attribute__((unavailable("error")));
@property id _Nullable openErrorVar __attribute__((swift_name("openErrorVar"))) __attribute__((unavailable("error")));
@property (readonly) id _Nullable openWarningVal __attribute__((swift_name("openWarningVal"))) __attribute__((unavailable("error")));
@property id _Nullable openWarningVar __attribute__((swift_name("openWarningVar"))) __attribute__((unavailable("error")));
@property (readonly) id _Nullable openNormalVal __attribute__((swift_name("openNormalVal"))) __attribute__((unavailable("error")));
@property id _Nullable openNormalVar __attribute__((swift_name("openNormalVar"))) __attribute__((unavailable("error")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.WarningOverride")))
@interface ValuesTestDeprecationWarningOverride : ValuesTestDeprecation
- (instancetype)initWithHidden:(int8_t)hidden __attribute__((swift_name("init(hidden:)"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("warning")));
- (instancetype)initWithError:(int16_t)error __attribute__((swift_name("init(error:)"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("warning")));
- (instancetype)initWithWarning:(int32_t)warning __attribute__((swift_name("init(warning:)"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("warning")));
- (instancetype)initWithNormal:(int64_t)normal __attribute__((swift_name("init(normal:)"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("warning")));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)openHidden __attribute__((swift_name("openHidden()"))) __attribute__((deprecated("warning")));
- (void)openError __attribute__((swift_name("openError()"))) __attribute__((deprecated("warning")));
- (void)openWarning __attribute__((swift_name("openWarning()"))) __attribute__((deprecated("warning")));
- (int32_t)openNormal __attribute__((swift_name("openNormal()"))) __attribute__((deprecated("warning")));
@property (readonly) id _Nullable openHiddenVal __attribute__((swift_name("openHiddenVal"))) __attribute__((deprecated("warning")));
@property id _Nullable openHiddenVar __attribute__((swift_name("openHiddenVar"))) __attribute__((deprecated("warning")));
@property (readonly) id _Nullable openErrorVal __attribute__((swift_name("openErrorVal"))) __attribute__((deprecated("warning")));
@property id _Nullable openErrorVar __attribute__((swift_name("openErrorVar"))) __attribute__((deprecated("warning")));
@property (readonly) id _Nullable openWarningVal __attribute__((swift_name("openWarningVal"))) __attribute__((deprecated("warning")));
@property id _Nullable openWarningVar __attribute__((swift_name("openWarningVar"))) __attribute__((deprecated("warning")));
@property (readonly) id _Nullable openNormalVal __attribute__((swift_name("openNormalVal"))) __attribute__((deprecated("warning")));
@property id _Nullable openNormalVar __attribute__((swift_name("openNormalVar"))) __attribute__((deprecated("warning")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDeprecation.NormalOverride")))
@interface ValuesTestDeprecationNormalOverride : ValuesTestDeprecation
- (instancetype)initWithHidden:(int8_t)hidden __attribute__((swift_name("init(hidden:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithError:(int16_t)error __attribute__((swift_name("init(error:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithWarning:(int32_t)warning __attribute__((swift_name("init(warning:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithNormal:(int64_t)normal __attribute__((swift_name("init(normal:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)openError __attribute__((swift_name("openError()"))) __attribute__((unavailable("Overrides deprecated member in 'conversions.TestDeprecation'. error")));
- (void)openWarning __attribute__((swift_name("openWarning()"))) __attribute__((deprecated("Overrides deprecated member in 'conversions.TestDeprecation'. warning")));
- (int32_t)openNormal __attribute__((swift_name("openNormal()")));
@property (readonly) id _Nullable openErrorVal __attribute__((swift_name("openErrorVal"))) __attribute__((unavailable("Overrides deprecated member in 'conversions.TestDeprecation'. error")));
@property id _Nullable openErrorVar __attribute__((swift_name("openErrorVar"))) __attribute__((unavailable("Overrides deprecated member in 'conversions.TestDeprecation'. error")));
@property (readonly) id _Nullable openWarningVal __attribute__((swift_name("openWarningVal"))) __attribute__((deprecated("Overrides deprecated member in 'conversions.TestDeprecation'. warning")));
@property id _Nullable openWarningVar __attribute__((swift_name("openWarningVar"))) __attribute__((deprecated("Overrides deprecated member in 'conversions.TestDeprecation'. warning")));
@property (readonly) id _Nullable openNormalVal __attribute__((swift_name("openNormalVal")));
@property id _Nullable openNormalVar __attribute__((swift_name("openNormalVar")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TopLevelHidden")))
@interface ValuesTopLevelHidden : NSObject
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TopLevelHidden.Nested")))
@interface ValuesTopLevelHiddenNested : NSObject
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TopLevelHidden.NestedNested")))
@interface ValuesTopLevelHiddenNestedNested : NSObject
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TopLevelHidden.NestedInner")))
@interface ValuesTopLevelHiddenNestedInner : NSObject
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TopLevelHidden.Inner")))
@interface ValuesTopLevelHiddenInner : NSObject
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TopLevelHidden.InnerInner")))
@interface ValuesTopLevelHiddenInnerInner : NSObject
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestWeakRefs")))
@interface ValuesTestWeakRefs : ValuesBase
- (instancetype)initWithFrozen:(BOOL)frozen __attribute__((swift_name("init(frozen:)"))) __attribute__((objc_designated_initializer));
- (id)getObj __attribute__((swift_name("getObj()")));
- (void)clearObj __attribute__((swift_name("clearObj()")));
- (NSArray<id> *)createCycle __attribute__((swift_name("createCycle()")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedRefs")))
@interface ValuesSharedRefs : ValuesBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (ValuesSharedRefsMutableData *)createRegularObject __attribute__((swift_name("createRegularObject()")));
- (void (^)(void))createLambda __attribute__((swift_name("createLambda()")));
- (NSMutableArray<id> *)createCollection __attribute__((swift_name("createCollection()")));
- (ValuesSharedRefsMutableData *)createFrozenRegularObject __attribute__((swift_name("createFrozenRegularObject()")));
- (void (^)(void))createFrozenLambda __attribute__((swift_name("createFrozenLambda()")));
- (NSMutableArray<id> *)createFrozenCollection __attribute__((swift_name("createFrozenCollection()")));
- (BOOL)hasAliveObjects __attribute__((swift_name("hasAliveObjects()")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedRefs.MutableData")))
@interface ValuesSharedRefsMutableData : ValuesBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)update __attribute__((swift_name("update()")));
@property int32_t x __attribute__((swift_name("x")));
@end;

__attribute__((swift_name("ClassForTypeCheck")))
@interface ValuesClassForTypeCheck : ValuesBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((swift_name("InterfaceForTypeCheck")))
@protocol ValuesInterfaceForTypeCheck
@required
@end;

__attribute__((swift_name("IAbstractInterface")))
@protocol ValuesIAbstractInterface
@required
- (int32_t)foo __attribute__((swift_name("foo()")));
@end;

__attribute__((swift_name("IAbstractInterface2")))
@protocol ValuesIAbstractInterface2
@required
- (int32_t)foo __attribute__((swift_name("foo()")));
@end;

__attribute__((swift_name("AbstractInterfaceBase")))
@interface ValuesAbstractInterfaceBase : ValuesBase <ValuesIAbstractInterface>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (int32_t)foo __attribute__((swift_name("foo()")));
- (int32_t)bar __attribute__((swift_name("bar()")));
@end;

__attribute__((swift_name("AbstractInterfaceBase2")))
@interface ValuesAbstractInterfaceBase2 : ValuesBase <ValuesIAbstractInterface2>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((swift_name("AbstractInterfaceBase3")))
@interface ValuesAbstractInterfaceBase3 : ValuesBase <ValuesIAbstractInterface>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (int32_t)foo __attribute__((swift_name("foo()")));
@end;

__attribute__((swift_name("GH3525Base")))
@interface ValuesGH3525Base : ValuesBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GH3525")))
@interface ValuesGH3525 : ValuesGH3525Base
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
+ (instancetype)gH3525 __attribute__((swift_name("init()")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestStringConversion")))
@interface ValuesTestStringConversion : ValuesBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property id str __attribute__((swift_name("str")));
@end;

@interface ValuesEnumeration (ValuesKt)
- (ValuesEnumeration *)getAnswer __attribute__((swift_name("getAnswer()")));
@end;

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ValuesKt")))
@interface ValuesValuesKt : ValuesBase
+ (ValuesBoolean * _Nullable)boxBooleanValue:(BOOL)booleanValue __attribute__((swift_name("box(booleanValue:)")));
+ (ValuesByte * _Nullable)boxByteValue:(int8_t)byteValue __attribute__((swift_name("box(byteValue:)")));
+ (ValuesShort * _Nullable)boxShortValue:(int16_t)shortValue __attribute__((swift_name("box(shortValue:)")));
+ (ValuesInt * _Nullable)boxIntValue:(int32_t)intValue __attribute__((swift_name("box(intValue:)")));
+ (ValuesLong * _Nullable)boxLongValue:(int64_t)longValue __attribute__((swift_name("box(longValue:)")));
+ (ValuesUByte * _Nullable)boxUByteValue:(uint8_t)uByteValue __attribute__((swift_name("box(uByteValue:)")));
+ (ValuesUShort * _Nullable)boxUShortValue:(uint16_t)uShortValue __attribute__((swift_name("box(uShortValue:)")));
+ (ValuesUInt * _Nullable)boxUIntValue:(uint32_t)uIntValue __attribute__((swift_name("box(uIntValue:)")));
+ (ValuesULong * _Nullable)boxULongValue:(uint64_t)uLongValue __attribute__((swift_name("box(uLongValue:)")));
+ (ValuesFloat * _Nullable)boxFloatValue:(float)floatValue __attribute__((swift_name("box(floatValue:)")));
+ (ValuesDouble * _Nullable)boxDoubleValue:(double)doubleValue __attribute__((swift_name("box(doubleValue:)")));
+ (void)ensureEqualBooleansActual:(ValuesBoolean * _Nullable)actual expected:(BOOL)expected __attribute__((swift_name("ensureEqualBooleans(actual:expected:)")));
+ (void)ensureEqualBytesActual:(ValuesByte * _Nullable)actual expected:(int8_t)expected __attribute__((swift_name("ensureEqualBytes(actual:expected:)")));
+ (void)ensureEqualShortsActual:(ValuesShort * _Nullable)actual expected:(int16_t)expected __attribute__((swift_name("ensureEqualShorts(actual:expected:)")));
+ (void)ensureEqualIntsActual:(ValuesInt * _Nullable)actual expected:(int32_t)expected __attribute__((swift_name("ensureEqualInts(actual:expected:)")));
+ (void)ensureEqualLongsActual:(ValuesLong * _Nullable)actual expected:(int64_t)expected __attribute__((swift_name("ensureEqualLongs(actual:expected:)")));
+ (void)ensureEqualUBytesActual:(ValuesUByte * _Nullable)actual expected:(uint8_t)expected __attribute__((swift_name("ensureEqualUBytes(actual:expected:)")));
+ (void)ensureEqualUShortsActual:(ValuesUShort * _Nullable)actual expected:(uint16_t)expected __attribute__((swift_name("ensureEqualUShorts(actual:expected:)")));
+ (void)ensureEqualUIntsActual:(ValuesUInt * _Nullable)actual expected:(uint32_t)expected __attribute__((swift_name("ensureEqualUInts(actual:expected:)")));
+ (void)ensureEqualULongsActual:(ValuesULong * _Nullable)actual expected:(uint64_t)expected __attribute__((swift_name("ensureEqualULongs(actual:expected:)")));
+ (void)ensureEqualFloatsActual:(ValuesFloat * _Nullable)actual expected:(float)expected __attribute__((swift_name("ensureEqualFloats(actual:expected:)")));
+ (void)ensureEqualDoublesActual:(ValuesDouble * _Nullable)actual expected:(double)expected __attribute__((swift_name("ensureEqualDoubles(actual:expected:)")));
+ (void)emptyFun __attribute__((swift_name("emptyFun()")));
+ (NSString *)strFun __attribute__((swift_name("strFun()")));
+ (id)argsFunI:(int32_t)i l:(int64_t)l d:(double)d s:(NSString *)s __attribute__((swift_name("argsFun(i:l:d:s:)")));
+ (NSString *)funArgumentFoo:(NSString *(^)(void))foo __attribute__((swift_name("funArgument(foo:)")));
+ (id _Nullable)genericFooT:(id _Nullable)t foo:(id _Nullable (^)(id _Nullable))foo __attribute__((swift_name("genericFoo(t:foo:)")));
+ (id)fooGenericNumberR:(id)r foo:(id (^)(id))foo __attribute__((swift_name("fooGenericNumber(r:foo:)")));
+ (NSArray<id> *)varargToListArgs:(ValuesKotlinArray *)args __attribute__((swift_name("varargToList(args:)")));
+ (NSString *)subExt:(NSString *)receiver i:(int32_t)i __attribute__((swift_name("subExt(_:i:)")));
+ (NSString *)toString:(id _Nullable)receiver __attribute__((swift_name("toString(_:)")));
+ (void)print:(id _Nullable)receiver __attribute__((swift_name("print(_:)")));
+ (id _Nullable)boxChar:(unichar)receiver __attribute__((swift_name("boxChar(_:)")));
+ (BOOL)isA:(id _Nullable)receiver __attribute__((swift_name("isA(_:)")));
+ (NSString *)iFunExt:(id<ValuesI>)receiver __attribute__((swift_name("iFunExt(_:)")));
+ (ValuesEnumeration *)passEnum __attribute__((swift_name("passEnum()")));
+ (void)receiveEnumE:(int32_t)e __attribute__((swift_name("receiveEnum(e:)")));
+ (ValuesEnumeration *)getValue:(int32_t)value __attribute__((swift_name("get(value:)")));
+ (ValuesWithCompanionAndObjectCompanion *)getCompanionObject __attribute__((swift_name("getCompanionObject()")));
+ (ValuesWithCompanionAndObjectNamed *)getNamedObject __attribute__((swift_name("getNamedObject()")));
+ (ValuesOpenClassI *)getNamedObjectInterface __attribute__((swift_name("getNamedObjectInterface()")));
+ (id)boxIc1:(int32_t)ic1 __attribute__((swift_name("box(ic1:)")));
+ (id)boxIc2:(id)ic2 __attribute__((swift_name("box(ic2:)")));
+ (id)boxIc3:(id _Nullable)ic3 __attribute__((swift_name("box(ic3:)")));
+ (NSString *)concatenateInlineClassValuesIc1:(int32_t)ic1 ic1N:(id _Nullable)ic1N ic2:(id)ic2 ic2N:(id _Nullable)ic2N ic3:(id _Nullable)ic3 ic3N:(id _Nullable)ic3N __attribute__((swift_name("concatenateInlineClassValues(ic1:ic1N:ic2:ic2N:ic3:ic3N:)")));
+ (int32_t)getValue1:(int32_t)receiver __attribute__((swift_name("getValue1(_:)")));
+ (ValuesInt * _Nullable)getValueOrNull1:(id _Nullable)receiver __attribute__((swift_name("getValueOrNull1(_:)")));
+ (NSString *)getValue2:(id)receiver __attribute__((swift_name("getValue2(_:)")));
+ (NSString * _Nullable)getValueOrNull2:(id _Nullable)receiver __attribute__((swift_name("getValueOrNull2(_:)")));
+ (ValuesTripleVals * _Nullable)getValue3:(id _Nullable)receiver __attribute__((swift_name("getValue3(_:)")));
+ (ValuesTripleVals * _Nullable)getValueOrNull3:(id _Nullable)receiver __attribute__((swift_name("getValueOrNull3(_:)")));
+ (BOOL)isFrozenObj:(id)obj __attribute__((swift_name("isFrozen(obj:)")));
+ (id)kotlinLambdaBlock:(id (^)(id))block __attribute__((swift_name("kotlinLambda(block:)")));
+ (int64_t)multiplyInt:(int32_t)int_ long:(int64_t)long_ __attribute__((swift_name("multiply(int:long:)")));
+ (id)same:(id)receiver __attribute__((swift_name("same(_:)")));
+ (ValuesInt * _Nullable)callBase1:(id<ValuesBase1>)base1 value:(ValuesInt * _Nullable)value __attribute__((swift_name("call(base1:value:)")));
+ (ValuesInt * _Nullable)callExtendedBase1:(id<ValuesExtendedBase1>)extendedBase1 value:(ValuesInt * _Nullable)value __attribute__((swift_name("call(extendedBase1:value:)")));
+ (ValuesInt * _Nullable)callBase2:(id<ValuesBase2>)base2 value:(ValuesInt * _Nullable)value __attribute__((swift_name("call(base2:value:)")));
+ (int32_t)callBase3:(id)base3 value:(ValuesInt * _Nullable)value __attribute__((swift_name("call(base3:value:)")));
+ (int32_t)callBase23:(ValuesBase23 *)base23 value:(ValuesInt * _Nullable)value __attribute__((swift_name("call(base23:value:)")));
+ (id<ValuesTransform>)createTransformDecimalStringToInt __attribute__((swift_name("createTransformDecimalStringToInt()")));
+ (BOOL)runUnitBlockBlock:(void (^)(void))block __attribute__((swift_name("runUnitBlock(block:)")));
+ (void (^)(void))asUnitBlockBlock:(id _Nullable (^)(void))block __attribute__((swift_name("asUnitBlock(block:)")));
+ (BOOL)runNothingBlockBlock:(void (^)(void))block __attribute__((swift_name("runNothingBlock(block:)")));
+ (void (^)(void))asNothingBlockBlock:(id _Nullable (^)(void))block __attribute__((swift_name("asNothingBlock(block:)")));
+ (void (^ _Nullable)(void))getNullBlock __attribute__((swift_name("getNullBlock()")));
+ (BOOL)isBlockNullBlock:(void (^ _Nullable)(void))block __attribute__((swift_name("isBlockNull(block:)")));
+ (BOOL)isFunctionObj:(id _Nullable)obj __attribute__((swift_name("isFunction(obj:)")));
+ (BOOL)isFunction0Obj:(id _Nullable)obj __attribute__((swift_name("isFunction0(obj:)")));
+ (void)takeForwardDeclaredClassObj:(ForwardDeclaredClass *)obj __attribute__((swift_name("takeForwardDeclaredClass(obj:)")));
+ (void)takeForwardDeclaredProtocolObj:(id<ForwardDeclared>)obj __attribute__((swift_name("takeForwardDeclaredProtocol(obj:)")));
+ (void)error __attribute__((swift_name("error()"))) __attribute__((unavailable("error")));
+ (void)warning __attribute__((swift_name("warning()"))) __attribute__((deprecated("warning")));
+ (void)gc __attribute__((swift_name("gc()")));
+ (BOOL)testClassTypeCheckX:(id)x __attribute__((swift_name("testClassTypeCheck(x:)")));
+ (BOOL)testInterfaceTypeCheckX:(id)x __attribute__((swift_name("testInterfaceTypeCheck(x:)")));
+ (int32_t)testAbstractInterfaceCallX:(id<ValuesIAbstractInterface>)x __attribute__((swift_name("testAbstractInterfaceCall(x:)")));
+ (int32_t)testAbstractInterfaceCall2X:(id<ValuesIAbstractInterface2>)x __attribute__((swift_name("testAbstractInterfaceCall2(x:)")));
@property (class, readonly) double dbl __attribute__((swift_name("dbl")));
@property (class, readonly) float flt __attribute__((swift_name("flt")));
@property (class, readonly) int32_t integer __attribute__((swift_name("integer")));
@property (class, readonly) int64_t longInt __attribute__((swift_name("longInt")));
@property (class) int32_t intVar __attribute__((swift_name("intVar")));
@property (class) NSString *str __attribute__((swift_name("str")));
@property (class) id strAsAny __attribute__((swift_name("strAsAny")));
@property (class) id minDoubleVal __attribute__((swift_name("minDoubleVal")));
@property (class) id maxDoubleVal __attribute__((swift_name("maxDoubleVal")));
@property (class, readonly) double nanDoubleVal __attribute__((swift_name("nanDoubleVal")));
@property (class, readonly) float nanFloatVal __attribute__((swift_name("nanFloatVal")));
@property (class, readonly) double infDoubleVal __attribute__((swift_name("infDoubleVal")));
@property (class, readonly) float infFloatVal __attribute__((swift_name("infFloatVal")));
@property (class, readonly) BOOL boolVal __attribute__((swift_name("boolVal")));
@property (class, readonly) id boolAnyVal __attribute__((swift_name("boolAnyVal")));
@property (class, readonly) NSArray<id> *numbersList __attribute__((swift_name("numbersList")));
@property (class, readonly) NSArray<id> *anyList __attribute__((swift_name("anyList")));
@property (class) id lateinitIntVar __attribute__((swift_name("lateinitIntVar")));
@property (class, readonly) NSString *lazyVal __attribute__((swift_name("lazyVal")));
@property (class) ValuesKotlinArray *delegatedGlobalArray __attribute__((swift_name("delegatedGlobalArray")));
@property (class, readonly) NSArray<NSString *> *delegatedList __attribute__((swift_name("delegatedList")));
@property (class, readonly) id _Nullable nullVal __attribute__((swift_name("nullVal")));
@property (class) NSString * _Nullable nullVar __attribute__((swift_name("nullVar")));
@property (class) id anyValue __attribute__((swift_name("anyValue")));
@property (class, readonly) ValuesInt *(^sumLambda)(ValuesInt *, ValuesInt *) __attribute__((swift_name("sumLambda")));
@property (class, readonly) int32_t PROPERTY_NAME_MUST_NOT_BE_ALTERED_BY_SWIFT __attribute__((swift_name("PROPERTY_NAME_MUST_NOT_BE_ALTERED_BY_SWIFT")));
@property (class, readonly) id _Nullable errorVal __attribute__((swift_name("errorVal"))) __attribute__((unavailable("error")));
@property (class) id _Nullable errorVar __attribute__((swift_name("errorVar"))) __attribute__((unavailable("error")));
@property (class, readonly) id _Nullable warningVal __attribute__((swift_name("warningVal"))) __attribute__((deprecated("warning")));
@property (class) id _Nullable warningVar __attribute__((swift_name("warningVar"))) __attribute__((deprecated("warning")));
@property (class) int32_t gh3525BaseInitCount __attribute__((swift_name("gh3525BaseInitCount")));
@property (class) int32_t gh3525InitCount __attribute__((swift_name("gh3525InitCount")));
@end;

