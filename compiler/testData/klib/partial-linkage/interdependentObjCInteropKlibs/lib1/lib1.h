#import <Foundation/Foundation.h>

@protocol UnmodifiedProtocol <NSObject>
@end

@interface UnmodifiedClass : NSObject
@end

@protocol RemovedProtocol <NSObject>
@end

@interface RemovedClass : NSObject
@end

@interface BaseClassToProtocolProtocol : NSObject
- (int)method:(int)value;
@end
