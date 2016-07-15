/**
  ******************************************************************************
  * @file    usbd_usr.h
  * @author  MCD Application Team
  * @version V1.0.0
  * @date    22-July-2011
  * @brief   Header file for usbd_usr.c
  ******************************************************************************
  * @attention
  *
  * THE PRESENT FIRMWARE WHICH IS FOR GUIDANCE ONLY AIMS AT PROVIDING CUSTOMERS
  * WITH CODING INFORMATION REGARDING THEIR PRODUCTS IN ORDER FOR THEM TO SAVE
  * TIME. AS A RESULT, STMICROELECTRONICS SHALL NOT BE HELD LIABLE FOR ANY
  * DIRECT, INDIRECT OR CONSEQUENTIAL DAMAGES WITH RESPECT TO ANY CLAIMS ARISING
  * FROM THE CONTENT OF SUCH FIRMWARE AND/OR THE USE MADE BY CUSTOMERS OF THE
  * CODING INFORMATION CONTAINED HEREIN IN CONNECTION WITH THEIR PRODUCTS.
  *
  * <h2><center>&copy; COPYRIGHT 2011 STMicroelectronics</center></h2>
  ******************************************************************************
  */ 

/* Define to prevent recursive inclusion -------------------------------------*/
#ifndef __USBD_USR_H__
#define __USBD_USR_H__

/* Includes ------------------------------------------------------------------*/
#include "usbd_core.h"


/** @addtogroup USBD_USER
  * @{
  */

/** @addtogroup USBD_MSC_DEMO_USER_CALLBACKS
  * @{
  */

/** @defgroup USBD_USR
  * @brief This file is the Header file for usbd_usr.c
  * @{
  */ 


/** @defgroup USBD_USR_Exported_Types
  * @{
  */ 

extern  USBD_Usr_cb_TypeDef USR_cb;
extern  USBD_Usr_cb_TypeDef USR_FS_cb;
extern  USBD_Usr_cb_TypeDef USR_HS_cb;



/**
  * @}
  */ 



/** @defgroup USBD_USR_Exported_Defines
  * @{
  */ 

/**
  * @}
  */ 

/** @defgroup USBD_USR_Exported_Macros
  * @{
  */ 
/**
  * @}
  */ 

/** @defgroup USBD_USR_Exported_Variables
  * @{
  */ 

void     USBD_USR_Init(void);
void     USBD_USR_DeviceReset (uint8_t speed);
void     USBD_USR_DeviceConfigured (void);
void     USBD_USR_DeviceSuspended(void);
void     USBD_USR_DeviceResumed(void);

void     USBD_USR_DeviceConnected(void);
void     USBD_USR_DeviceDisconnected(void); 

void     USBD_USR_FS_Init(void);
void     USBD_USR_FS_DeviceReset (uint8_t speed);
void     USBD_USR_FS_DeviceConfigured (void);
void     USBD_USR_FS_DeviceSuspended(void);
void     USBD_USR_FS_DeviceResumed(void);

void     USBD_USR_FS_DeviceConnected(void);
void     USBD_USR_FS_DeviceDisconnected(void);  

void     USBD_USR_HS_Init(void);
void     USBD_USR_HS_DeviceReset (uint8_t speed);
void     USBD_USR_HS_DeviceConfigured (void);
void     USBD_USR_HS_DeviceSuspended(void);
void     USBD_USR_HS_DeviceResumed(void);

void     USBD_USR_HS_DeviceConnected(void);
void     USBD_USR_HS_DeviceDisconnected(void);  

/**
  * @}
  */ 

/** @defgroup USBD_USR_Exported_FunctionsPrototype
  * @{
  */ 
/**
  * @}
  */ 

#endif /*__USBD_USR_H__*/

/**
  * @}
  */ 

/**
  * @}
  */ 

/**
  * @}
  */ 

/******************* (C) COPYRIGHT 2011 STMicroelectronics *****END OF FILE****/




