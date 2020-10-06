#import <Foundation/NSObject.h>

@interface WeakRefHolder : NSObject
@property (weak) id obj;
-(void)loadManyTimes;
@end;