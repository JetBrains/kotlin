#import <Foundation/Foundation.h>
#import "lib1.h"

@interface ClassWithUnmodifiedProtocol : NSObject <UnmodifiedProtocol>
+ (int)staticMethod:(int)value;
@end

@interface ClassWithRemovedProtocol : NSObject <RemovedProtocol>
+ (int)staticMethod:(int)value;
@end

@interface ClassWithRemovedBase : RemovedClass
@end

@interface ClassWithBaseClassToProtocol : BaseClassToProtocolProtocol
@end

@interface RemovedClassWrapper : NSObject
@property RemovedClass *c;
@end
