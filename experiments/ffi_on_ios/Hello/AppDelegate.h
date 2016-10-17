//
//  AppDelegate.h
//  Hello
//
//  Created by jetbrains on 14/10/16.
//  Copyright © 2016 jetbrains. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <CoreData/CoreData.h>

@interface AppDelegate : UIResponder <UIApplicationDelegate>

@property (strong, nonatomic) UIWindow *window;

@property (readonly, strong) NSPersistentContainer *persistentContainer;

- (void)saveContext;


@end

